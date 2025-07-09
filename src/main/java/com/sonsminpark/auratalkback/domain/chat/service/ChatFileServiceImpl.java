package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatFileUploadRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileDownloadResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatFile;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatMessage;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.MessageType;
import com.sonsminpark.auratalkback.domain.chat.exception.*;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatFileRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatMessageRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.global.s3.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFileServiceImpl implements ChatFileService {

    private final ChatFileRepository chatFileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int DOWNLOAD_URL_EXPIRY_HOURS = 1; // 1시간

    @Override
    @Transactional(readOnly = true)
    public void validateChatRoomAccess(Long chatroomId, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatroomId);

        if (!chatRoom.isActive()) {
            throw InvalidChatRoomStateException.deactivated();
        }

        if (chatRoom.isBannedUser(userId)) {
            throw ChatAccessDeniedException.of("강퇴된 채팅방에는 파일을 업로드할 수 없습니다.");
        }

        boolean isUserInChatRoom = chatRoomUserRepository.findByChatRoomIdAndUserId(chatroomId, userId).isPresent();
        if (!isUserInChatRoom) {
            throw ChatAccessDeniedException.of("채팅방에 참여하고 있지 않습니다.");
        }
    }

    @Override
    @Transactional
    public ChatFileResponseDto completeFileUpload(Long chatroomId, Long userId, ChatFileUploadRequestDto requestDto) {
        ChatRoom chatRoom = findChatRoomById(chatroomId);
        User uploader = findUserById(userId);

        validateChatRoomAccess(chatroomId, userId);

        if (requestDto.getFileSize() > MAX_FILE_SIZE) {
            throw ChatFileUploadException.fileSizeExceeded(MAX_FILE_SIZE);
        }

        String originalFileName = requestDto.getOriginalFileName();
        String fileExtension = FileUtil.extractExtension(originalFileName);
        String mimeType = FileUtil.getMimeType(fileExtension);
        String s3Url = "https://" + bucketName + ".s3.amazonaws.com/" + requestDto.getS3Key();

        ChatFile chatFile = ChatFile.builder()
                .chatRoom(chatRoom)
                .uploader(uploader)
                .originalFileName(originalFileName)
                .s3Key(requestDto.getS3Key())
                .s3Url(s3Url)
                .fileExtension(fileExtension)
                .mimeType(mimeType)
                .fileSize(requestDto.getFileSize())
                .build();

        ChatFile savedChatFile = chatFileRepository.save(chatFile);

        MessageType messageType = determineMessageType(mimeType);
        String messageContent = originalFileName;

        ChatMessage fileMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(uploader)
                .content(messageContent)
                .type(messageType)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(fileMessage);

        savedChatFile.setMessage(savedMessage);

        chatRoom.updateLastMessageAt();

        String uploaderThumbnailUrl = uploader.getUserProfileImage() != null ?
                uploader.getUserProfileImage().getThumbnailImageUrl() : null;

        ChatFileResponseDto responseDto = ChatFileResponseDto.from(savedChatFile, uploaderThumbnailUrl);

        messagingTemplate.convertAndSend("/topic/chatroom/" + chatroomId + "/files", responseDto);

        log.info("파일 업로드 완료 - 파일 ID: {}, 채팅방: {}, 업로더: {}",
                savedChatFile.getId(), chatroomId, userId);

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatFileDownloadResponseDto generateDownloadUrl(Long fileId, Long userId) {
        ChatFile chatFile = chatFileRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> ChatFileNotFoundException.of("파일을 찾을 수 없습니다."));

        validateChatRoomAccess(chatFile.getChatRoom().getId(), userId);

        // PresignedURL 생성
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(chatFile.getS3Key())
                .responseContentDisposition("attachment; filename=\"" + chatFile.getOriginalFileName() + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofHours(DOWNLOAD_URL_EXPIRY_HOURS))
                .build();

        URL url = s3Presigner.presignGetObject(presignRequest).url();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(DOWNLOAD_URL_EXPIRY_HOURS);

        return ChatFileDownloadResponseDto.builder()
                .fileId(fileId)
                .originalFileName(chatFile.getOriginalFileName())
                .downloadUrl(url.toString())
                .expiresAt(expiresAt)
                .fileSize(chatFile.getFileSize())
                .formattedFileSize(chatFile.getFormattedFileSize())
                .mimeType(chatFile.getMimeType())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatFileResponseDto> getChatRoomFiles(Long chatroomId, Long userId, int page, int size) {
        validateChatRoomAccess(chatroomId, userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ChatFile> chatFiles = chatFileRepository.findByChatRoomIdWithUploader(chatroomId, pageable);

        return chatFiles.stream()
                .map(chatFile -> {
                    String uploaderThumbnailUrl = chatFile.getUploader().getUserProfileImage() != null ?
                            chatFile.getUploader().getUserProfileImage().getThumbnailImageUrl() : null;
                    return ChatFileResponseDto.from(chatFile, uploaderThumbnailUrl);
                })
                .toList();
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        ChatFile chatFile = chatFileRepository.findByIdAndUploaderIdAndIsDeletedFalse(fileId, userId)
                .orElseThrow(() -> ChatFileNotFoundException.of("파일을 찾을 수 없거나 삭제 권한이 없습니다."));

        chatFile.softDelete();

        if (chatFile.getMessage() != null) {
            chatFile.getMessage().softDelete();
        }

        messagingTemplate.convertAndSend("/topic/chatroom/" + chatFile.getChatRoom().getId() + "/files/deleted",
                ChatFileResponseDto.from(chatFile));

        log.info("파일 삭제 완료 - 파일 ID: {}, 사용자: {}", fileId, userId);
    }

    private ChatRoom findChatRoomById(Long chatroomId) {
        return chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatroomId));
    }

    private User findUserById(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));
    }

    private MessageType determineMessageType(String mimeType) {
        if (mimeType.startsWith("image/")) {
            return MessageType.IMAGE;
        } else if (mimeType.startsWith("video/")) {
            return MessageType.VIDEO;
        } else if (mimeType.startsWith("audio/")) {
            return MessageType.VOICE;
        } else {
            return MessageType.FILE;
        }
    }
}