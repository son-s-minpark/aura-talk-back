package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomUpdateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.*;
import com.sonsminpark.auratalkback.domain.chat.entity.*;
import com.sonsminpark.auratalkback.domain.chat.exception.*;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatInvitationRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatMessageRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.domain.user.service.UserProfileImageService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatInvitationRepository chatInvitationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserProfileImageService userProfileImageService;
    private final ChatRoomImageService chatRoomImageService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final int DEFAULT_GROUP_IMAGE_COUNT = 2;

    private record DefaultGroupImage(String originalUrl, String thumbnailUrl) {
    }

    private DefaultGroupImage getDefaultGroupImage(Long chatRoomId) {
        try {
            // 항상 1 또는 2만 반환
            int index = (int) (chatRoomId % DEFAULT_GROUP_IMAGE_COUNT) + 1;

            // 범위 체크
            if (index < 1 || index > DEFAULT_GROUP_IMAGE_COUNT) {
                index = 1; // 기본값
            }

            String prefix = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/";

            log.debug("기본 그룹 이미지 생성 - 채팅방 ID: {}, 계산된 인덱스: {}", chatRoomId, index);

            String originalUrl = prefix + index + ".png";
            String thumbnailUrl = prefix + index + "_thumb.png";

            log.debug("생성된 이미지 URL - 원본: {}, 썸네일: {}", originalUrl, thumbnailUrl);

            return new DefaultGroupImage(originalUrl, thumbnailUrl);
        } catch (Exception e) {
            log.error("기본 그룹 이미지 생성 실패 - 채팅방 ID: {}, 에러: {}", chatRoomId, e.getMessage(), e);

            // 오류 발생 시 항상 1번 이미지 사용
            String prefix = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/";
            return new DefaultGroupImage(
                    prefix + "1.png",
                    prefix + "1_thumb.png"
            );
        }
    }

    @Override
    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto, Long userId) {
        User owner = findUserById(userId);

        String roomImageUrl = requestDto.getRoomImageUrl();
        String roomThumbnailImageUrl = requestDto.getRoomThumbnailImageUrl();

        if (roomImageUrl != null && !roomImageUrl.trim().isEmpty()) {
            log.info("채팅방 이미지 설정 - 원본: {}, 썸네일: {}", roomImageUrl, roomThumbnailImageUrl);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .name(requestDto.getName())
                .type(ChatRoomType.GROUP)
                .owner(owner)
                .roomImageUrl(roomImageUrl)
                .roomThumbnailImageUrl(roomThumbnailImageUrl)
                .isActive(true)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 기본 이미지 설정 (사용자가 이미지를 제공하지 않은 경우)
        if (roomImageUrl == null || roomImageUrl.trim().isEmpty()) {
            try {
                ChatRoomImageResponseDto defaultImage = chatRoomImageService.getDefaultImage(savedChatRoom.getId());
                savedChatRoom.updateRoomImage(defaultImage.getOriginalImageUrl(), defaultImage.getThumbnailImageUrl());
                log.info("채팅방 기본 이미지 설정 완료 - ID: {}, 원본: {}, 썸네일: {}",
                        savedChatRoom.getId(), defaultImage.getOriginalImageUrl(), defaultImage.getThumbnailImageUrl());
            } catch (Exception e) {
                log.error("채팅방 기본 이미지 설정 실패 - ID: {}, 에러: {}", savedChatRoom.getId(), e.getMessage());
                String fallbackOriginalUrl = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/1.png";
                String fallbackThumbnailUrl = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/1_thumb.png";
                savedChatRoom.updateRoomImage(fallbackOriginalUrl, fallbackThumbnailUrl);
                log.warn("채팅방 기본 이미지를 fallback으로 설정 - ID: {}", savedChatRoom.getId());
            }
        }

        // 방장을 채팅방에 추가
        addUserToChatRoom(savedChatRoom, owner);

        // 초대할 사용자들이 있으면 추가
        if (requestDto.getUserIds() != null && !requestDto.getUserIds().isEmpty()) {
            List<User> inviteUsers = userRepository.findAllByIdWithProfileImage(requestDto.getUserIds());
            for (User inviteUser : inviteUsers) {
                try {
                    addUserToChatRoom(savedChatRoom, inviteUser);
                    sendSystemMessage(savedChatRoom, inviteUser.getNickname() + "님이 초대되었습니다.");
                    log.info("사용자 {}를 채팅방 {}에 초대했습니다.", inviteUser.getId(), savedChatRoom.getId());
                } catch (Exception e) {
                    log.warn("사용자 {} 초대 실패: {}", inviteUser.getId(), e.getMessage());
                }
            }
        }

        log.info("채팅방 생성 완료 - ID: {}, 방장: {}, 이미지 설정: {}",
                savedChatRoom.getId(), userId, roomImageUrl != null ? "사용자 제공" : "기본 이미지");
        return buildChatRoomResponse(savedChatRoom, userId);
    }

    @Override
    @Transactional
    public ChatRoomResponseDto createOneToOneChatRoom(Long userId, Long targetUserId) {
        User user = findUserById(userId);
        User targetUser = findUserById(targetUserId);

        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신과는 채팅할 수 없습니다.");
        }

        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findOneToOneChatRoom(
                ChatRoomType.ONE_TO_ONE, userId, targetUserId);

        if (existingChatRoom.isPresent()) {
            ChatRoom chatRoom = existingChatRoom.get();
            if (chatRoom.isActive()) {
                log.info("기존 1:1 채팅방 반환 - ID: {}, 사용자: {} <-> {}",
                        chatRoom.getId(), userId, targetUserId);
                return buildChatRoomResponse(chatRoom, userId);
            }
        }

        String chatRoomName = user.getNickname() + ", " + targetUser.getNickname();

        ChatRoom chatRoom = ChatRoom.builder()
                .name(chatRoomName)
                .type(ChatRoomType.ONE_TO_ONE)
                .owner(user)
                .isActive(true)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        addUserToChatRoom(savedChatRoom, user);
        addUserToChatRoom(savedChatRoom, targetUser);

        log.info("1:1 채팅방 생성 완료 - ID: {}, 사용자: {} <-> {}",
                savedChatRoom.getId(), userId, targetUserId);

        return buildChatRoomResponse(savedChatRoom, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponseDto> getChatRoomsByUserId(Long userId) {
        validateUserExists(userId);

        List<ChatRoom> chatRooms = chatRoomRepository.findActiveByUserIdWithOwner(userId);

        return chatRooms.stream()
                .map(chatRoom -> buildChatRoomResponse(chatRoom, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatRoomResponseDto> getChatRoomsByUserId(Long userId, int page, int size) {
        validateUserExists(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));

        Page<ChatRoom> chatRoomPage = chatRoomRepository.findActiveByUserIdWithOwnerPaging(userId, pageable);

        return chatRoomPage.map(chatRoom -> buildChatRoomResponse(chatRoom, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomResponseDto getChatRoomInfo(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithOwner(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));

        validateChatRoomAccess(chatRoomId, userId);

        // 채팅 메시지 총 개수 조회
        long totalMessages = chatMessageRepository.countByChatRoomIdAndIsDeletedFalse(chatRoomId);

        // 기본 페이지 크기 (50개)로 총 페이지 수 계산
        int defaultPageSize = 50;
        int totalPages = (int) Math.ceil((double) totalMessages / defaultPageSize);

        return buildChatRoomResponseWithTotalPages(chatRoom, userId, totalPages);
    }

    @Override
    @Transactional
    public ChatRoomResponseDto updateChatRoom(Long chatRoomId, ChatRoomUpdateRequestDto requestDto, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        validateOwnerPermission(chatRoom, userId);
        validateChatRoomActive(chatRoom);

        if (requestDto.getName() != null && !requestDto.getName().trim().isEmpty()) {
            chatRoom.updateName(requestDto.getName().trim());
//            sendSystemMessage(chatRoom, "채팅방 이름이 '" + requestDto.getName() + "'로 변경되었습니다.");
        }

        if (requestDto.getRoomImageUrl() != null) {
            if (requestDto.getRoomThumbnailImageUrl() != null) {
                chatRoom.updateRoomImage(requestDto.getRoomImageUrl(), requestDto.getRoomThumbnailImageUrl());
            } else {
                chatRoom.updateRoomImage(requestDto.getRoomImageUrl());
            }
//            sendSystemMessage(chatRoom, "채팅방 이미지가 변경되었습니다.");
        }

        log.info("채팅방 정보 수정 완료 - ID: {}, 수정자: {}", chatRoomId, userId);
        return buildChatRoomResponse(chatRoom, userId);
    }

    @Override
    @Transactional
    public ChatRoomResponseDto deleteRoomImage(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        validateOwnerPermission(chatRoom, userId);
        validateChatRoomActive(chatRoom);

        // 현재 이미지 URL들 저장
        String currentImageUrl = chatRoom.getRoomImageUrl();
        String currentThumbnailUrl = chatRoom.getRoomThumbnailImageUrl();

        // 채팅방 이미지 삭제 및 기본 이미지로 변경
        ChatRoomImageResponseDto defaultImageDto = chatRoomImageService.deleteRoomImage(
                chatRoomId, currentImageUrl, currentThumbnailUrl);

        // 채팅방의 이미지 URL들을 기본 이미지로 업데이트
        chatRoom.updateRoomImage(defaultImageDto.getOriginalImageUrl(), defaultImageDto.getThumbnailImageUrl());

        // 시스템 메시지 전송
//        sendSystemMessage(chatRoom, "채팅방 이미지가 기본 이미지로 변경되었습니다.");

        log.info("채팅방 이미지 삭제 완료 - ID: {}, 수정자: {}", chatRoomId, userId);
        return buildChatRoomResponse(chatRoom, userId);
    }

    @Override
    @Transactional
    public void leaveChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        User user = findUserById(userId);

        ChatRoomUser roomUser = findChatRoomUser(chatRoomId, userId);

        if (chatRoom.isUserOwner(userId)) {
            handleOwnerLeaving(chatRoom);
        } else {
            sendSystemMessage(chatRoom, user.getNickname() + "님이 채팅방을 나갔습니다.");
        }

        chatRoomUserRepository.delete(roomUser);
        log.info("사용자 {}가 채팅방 {}을 나갔습니다.", userId, chatRoomId);
    }

    @Override
    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        validateOwnerPermission(chatRoom, userId);

        sendSystemMessage(chatRoom, "채팅방이 삭제되었습니다.");

        // 채팅방과 관련된 모든 데이터 삭제
        chatRoomUserRepository.deleteAllByChatRoomId(chatRoomId);
        chatMessageRepository.deleteAllByChatRoomId(chatRoomId);
        chatRoomRepository.delete(chatRoom);

        log.info("채팅방 {}이 완전히 삭제되었습니다.", chatRoomId);
    }

    @Override
    @Transactional
    public void kickUser(Long chatRoomId, Long ownerId, Long targetUserId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        User targetUser = findUserById(targetUserId);

        validateOwnerPermission(chatRoom, ownerId);
        validateChatRoomActive(chatRoom);

        if (ownerId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 강퇴할 수 없습니다.");
        }

        ChatRoomUser roomUser = findChatRoomUser(chatRoomId, targetUserId);

        chatRoom.banUser(targetUser);
        sendSystemMessage(chatRoom, targetUser.getNickname() + "님이 강퇴되었습니다.");
        chatRoomUserRepository.delete(roomUser);
        sendDirectMessage(targetUser, "'" + chatRoom.getName() + "' 채팅방에서 강퇴되었습니다.");

        log.info("사용자 {}가 채팅방 {}에서 강퇴되었습니다.", targetUserId, chatRoomId);
    }

    @Override
    @Transactional
    public void unbanUser(Long chatRoomId, Long ownerId, Long targetUserId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        User targetUser = findUserById(targetUserId);

        validateOwnerPermission(chatRoom, ownerId);
        validateChatRoomActive(chatRoom);

        if (ownerId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신의 강퇴를 해제할 수 없습니다.");
        }

        if (!chatRoom.isBannedUser(targetUserId)) {
            throw new IllegalArgumentException("강퇴되지 않은 사용자입니다.");
        }

        chatRoom.unbanUser(targetUser);
//        sendSystemMessage(chatRoom, targetUser.getNickname() + "님의 강퇴가 해제되었습니다.");

        log.info("사용자 {}의 채팅방 {} 강퇴가 해제되었습니다.", targetUserId, chatRoomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatUserResponseDto> getBannedUsers(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithBannedUsers(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));

        // 방장만 차단된 사용자 목록을 조회할 수 있음
        validateOwnerPermission(chatRoom, userId);

        return chatRoom.getBannedUsers().stream()
                .map(user -> {
                    String thumbnailUrl = user.getUserProfileImage() != null ?
                            user.getUserProfileImage().getThumbnailImageUrl() : null;
                    return ChatUserResponseDto.from(user, thumbnailUrl);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponseDto> searchChatRooms(String keyword, Long userId) {
        validateUserExists(userId);

        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        List<ChatRoom> searchResults = chatRoomRepository.searchByNameAndUserId(keyword.trim(), userId);

        return searchResults.stream()
                .map(chatRoom -> buildChatRoomResponse(chatRoom, userId))
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageResponseDto sendMessage(Long chatRoomId, ChatMessageRequestDto requestDto, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        User sender = findUserById(userId);

        validateChatRoomAccess(chatRoomId, userId);
        validateChatRoomActive(chatRoom);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(requestDto.getContent())
                .type(requestDto.getType())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        chatRoom.updateLastMessageAt();

        String senderThumbnailUrl = sender.getUserProfileImage() != null ?
                sender.getUserProfileImage().getThumbnailImageUrl() : null;

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(savedMessage, senderThumbnailUrl);

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponseDto> getMessages(Long chatRoomId, Long userId, Pageable pageable) {
        validateChatRoomAccess(chatRoomId, userId);

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomIdWithSender(chatRoomId, pageable);

        return messages.map(message -> {
            String senderThumbnailUrl = null;
            if (message.getSender() != null && message.getSender().getUserProfileImage() != null) {
                senderThumbnailUrl = message.getSender().getUserProfileImage().getThumbnailImageUrl();
            }
            return ChatMessageResponseDto.from(message, senderThumbnailUrl);
        });
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findByIdAndSenderId(messageId, userId)
                .orElseThrow(() -> MessageNotFoundException.of(messageId));

        message.softDelete();

        String senderThumbnailUrl = null;
        if (message.getSender() != null) {
            try {
                senderThumbnailUrl = userProfileImageService.getProfileImage(message.getSender().getId()).getThumbnailImageUrl();
            } catch (Exception e) {
                log.warn("프로필 이미지 조회 실패 - 사용자: {}, 오류: {}", message.getSender().getId(), e.getMessage());
            }
        }

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(message, senderThumbnailUrl);
        messagingTemplate.convertAndSend("/topic/chatroom/" + message.getChatRoom().getId(), responseDto);

        log.info("사용자 {}가 메시지 {}를 삭제했습니다.", userId, messageId);
    }

    @Override
    @Transactional
    public ChatInviteResponseDto createInviteLink(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        // 채팅방 접근 권한 검증
        validateChatRoomAccess(chatRoomId, userId);
        validateChatRoomActive(chatRoom);

        String inviteCode = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        chatRoom.generateInviteCode(inviteCode, expiresAt);

        String inviteLink = "https://auratalk.com/invite/" + inviteCode;

        log.info("채팅방 {} 초대 링크가 생성되었습니다. (만료: {})", chatRoomId, expiresAt);

        return ChatInviteResponseDto.builder()
                .inviteCode(inviteCode)
                .inviteLink(inviteLink)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    @Transactional
    public ChatInviteResponseDto sendInviteToFriend(Long chatRoomId, ChatInviteRequestDto requestDto, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        User inviter = findUserById(userId);
        User invitee = findUserById(requestDto.getUserId());

        // 채팅방 접근 권한 검증
        validateChatRoomAccess(chatRoomId, userId);
        validateChatRoomActive(chatRoom);

        if (isUserInChatRoom(chatRoomId, invitee.getId())) {
            throw InvalidChatRoomStateException.alreadyMember();
        }

        if (chatRoom.isBannedUser(invitee.getId())) {
            throw new IllegalArgumentException("강퇴된 사용자는 초대할 수 없습니다.");
        }

        ChatInviteResponseDto inviteResponse;
        if (chatRoom.isInviteCodeValid()) {
            inviteResponse = ChatInviteResponseDto.builder()
                    .inviteCode(chatRoom.getInviteCode())
                    .inviteLink("https://auratalk.com/invite/" + chatRoom.getInviteCode())
                    .expiresAt(chatRoom.getInviteCodeExpiredAt())
                    .build();
        } else {
            String inviteCode = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

            chatRoom.generateInviteCode(inviteCode, expiresAt);

            inviteResponse = ChatInviteResponseDto.builder()
                    .inviteCode(inviteCode)
                    .inviteLink("https://auratalk.com/invite/" + inviteCode)
                    .expiresAt(expiresAt)
                    .build();
        }

        String inviteMessage = inviter.getNickname() + "님이 '" + chatRoom.getName() + "' 채팅방에 초대했습니다.\n" +
                "링크: " + inviteResponse.getInviteLink();
        sendDirectMessage(invitee, inviteMessage);

        log.info("사용자 {}가 사용자 {}에게 채팅방 {} 초대 링크를 전송했습니다.", userId, requestDto.getUserId(), chatRoomId);

        return inviteResponse;
    }

    @Override
    @Transactional
    public void acceptInvite(String inviteCode, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode)
                .orElseThrow(() -> ChatInvitationException.invalidInviteCode());

        if (!chatRoom.isInviteCodeValid()) {
            throw ChatInvitationException.expiredInvite();
        }

        if (chatRoom.isBannedUser(userId)) {
            throw ChatRoomBannedException.of(chatRoom.getName());
        }

        User user = findUserById(userId);

        if (isUserInChatRoom(chatRoom.getId(), user.getId())) {
            throw InvalidChatRoomStateException.alreadyMember();
        }

        addUserToChatRoom(chatRoom, user);
        sendSystemMessage(chatRoom, user.getNickname() + "님이 채팅방에 입장했습니다.");

        log.info("사용자 {}가 초대 링크를 통해 채팅방 {}에 입장했습니다.", userId, chatRoom.getId());
    }

    @Override
    @Transactional
    public void updateNotificationSettings(Long chatRoomId, Long userId, boolean enabled) {
        ChatRoomUser roomUser = findChatRoomUser(chatRoomId, userId);
        roomUser.updateNotificationSetting(enabled);

        log.info("사용자 {}의 채팅방 {} 알림 설정이 {}로 변경되었습니다.", userId, chatRoomId, enabled ? "활성화" : "비활성화");
    }

    private User findUserById(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));
    }

    private ChatRoom findChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));
    }

    private ChatRoomUser findChatRoomUser(Long chatRoomId, Long userId) {
        List<ChatRoomUser> users = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
        if (users.isEmpty()) {
            throw InvalidChatRoomStateException.notMember();
        }
        return users.get(0);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException.of(userId);
        }
    }

    private void validateChatRoomActive(ChatRoom chatRoom) {
        if (!chatRoom.isActive()) {
            throw InvalidChatRoomStateException.deactivated();
        }
    }

    private void validateChatRoomAccess(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        if (chatRoom.isBannedUser(userId)) {
            throw ChatRoomBannedException.of();
        }

        if (!isUserInChatRoom(chatRoomId, userId)) {
            throw ChatAccessDeniedException.of("채팅방에 참여할 권한이 없습니다.");
        }
    }

    private void validateOwnerPermission(ChatRoom chatRoom, Long userId) {
        if (!chatRoom.isUserOwner(userId)) {
            throw InvalidChatRoomStateException.ownerRequired();
        }
    }

    private boolean isUserInChatRoom(Long chatRoomId, Long userId) {
        // findUserSettings 대신 findByChatRoomIdAndUserId 사용으로 중복 데이터 문제 해결
        List<ChatRoomUser> users = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
        return !users.isEmpty();
    }

    private void addUserToChatRoom(ChatRoom chatRoom, User user) {
        ChatRoomUser roomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .notificationEnabled(true)
                .build();
        chatRoomUserRepository.save(roomUser);
    }

    private ChatRoomResponseDto buildChatRoomResponse(ChatRoom chatRoom, Long currentUserId) {
        try {
            ChatRoomResponseDto dto = ChatRoomResponseDto.from(chatRoom, currentUserId);

            List<ChatRoomUser> roomUsers = chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(chatRoom.getId());

            List<ChatUserResponseDto> users = roomUsers.stream()
                    .map(roomUser -> {
                        User user = roomUser.getUser();
                        String thumbnailUrl = user.getUserProfileImage() != null ?
                                user.getUserProfileImage().getThumbnailImageUrl() : null;
                        return ChatUserResponseDto.from(user, thumbnailUrl);
                    })
                    .toList();

            dto.setUsers(users);

            if (chatRoom.getOwner() != null && chatRoom.getOwner().getUserProfileImage() != null) {
                dto.setOwnerThumbnailUrl(chatRoom.getOwner().getUserProfileImage().getThumbnailImageUrl());
            }

            // 그룹 채팅방인 경우 기본 이미지 설정 (DB에 없는 경우에만)
            if (chatRoom.getType() == ChatRoomType.GROUP &&
                    (chatRoom.getRoomImageUrl() == null || chatRoom.getRoomImageUrl().trim().isEmpty())) {

                try {
                    ChatRoomImageResponseDto defaultImage = chatRoomImageService.getDefaultImage(chatRoom.getId());

                    return ChatRoomResponseDto.builder()
                            .id(dto.getId())
                            .name(dto.getName())
                            .type(dto.getType())
                            .owner(dto.getOwner())
                            .users(dto.getUsers())
                            .createdAt(dto.getCreatedAt())
                            .lastMessageAt(dto.getLastMessageAt())
                            .isActive(dto.isActive())
                            .roomImageUrl(defaultImage.getOriginalImageUrl())
                            .roomThumbnailImageUrl(defaultImage.getThumbnailImageUrl())
                            .isOwner(dto.isOwner())
                            .inviteCode(dto.getInviteCode())
                            .inviteCodeExpiredAt(dto.getInviteCodeExpiredAt())
                            .build();
                } catch (Exception e) {
                    log.error("기본 그룹 이미지 설정 실패 - 채팅방 ID: {}, 에러: {}", chatRoom.getId(), e.getMessage(), e);
                    // 기본 이미지 설정 실패 시 원본 DTO 반환
                    return dto;
                }
            }

            return dto;
        } catch (Exception e) {
            log.error("채팅방 응답 생성 실패 - 채팅방 ID: {}, 에러: {}", chatRoom.getId(), e.getMessage(), e);

            return ChatRoomResponseDto.builder()
                    .id(chatRoom.getId())
                    .name(chatRoom.getName())
                    .type(chatRoom.getType())
                    .owner(chatRoom.getOwner() != null ? ChatUserResponseDto.from(chatRoom.getOwner()) : null)
                    .users(List.of())
                    .createdAt(chatRoom.getCreatedAt())
                    .lastMessageAt(chatRoom.getLastMessageAt())
                    .isActive(chatRoom.isActive())
                    .roomImageUrl(chatRoom.getRoomImageUrl())
                    .roomThumbnailImageUrl(chatRoom.getRoomThumbnailImageUrl())
                    .isOwner(chatRoom.isUserOwner(currentUserId))
                    .inviteCode(chatRoom.getInviteCode())
                    .inviteCodeExpiredAt(chatRoom.getInviteCodeExpiredAt())
                    .build();
        }
    }

    private ChatRoomResponseDto buildChatRoomResponseWithTotalPages(ChatRoom chatRoom, Long currentUserId, Integer totalPages) {
        try {
            ChatRoomResponseDto dto = ChatRoomResponseDto.from(chatRoom, currentUserId, totalPages);

            List<ChatRoomUser> roomUsers = chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(chatRoom.getId());

            List<ChatUserResponseDto> users = roomUsers.stream()
                    .map(roomUser -> {
                        User user = roomUser.getUser();
                        String thumbnailUrl = user.getUserProfileImage() != null ?
                                user.getUserProfileImage().getThumbnailImageUrl() : null;
                        return ChatUserResponseDto.from(user, thumbnailUrl);
                    })
                    .toList();

            dto.setUsers(users);

            if (chatRoom.getOwner() != null && chatRoom.getOwner().getUserProfileImage() != null) {
                dto.setOwnerThumbnailUrl(chatRoom.getOwner().getUserProfileImage().getThumbnailImageUrl());
            }

            // 그룹 채팅방인 경우 기본 이미지 설정 (DB에 없는 경우에만)
            if (chatRoom.getType() == ChatRoomType.GROUP &&
                    (chatRoom.getRoomImageUrl() == null || chatRoom.getRoomImageUrl().trim().isEmpty())) {

                try {
                    ChatRoomImageResponseDto defaultImage = chatRoomImageService.getDefaultImage(chatRoom.getId());

                    return ChatRoomResponseDto.builder()
                            .id(dto.getId())
                            .name(dto.getName())
                            .type(dto.getType())
                            .owner(dto.getOwner())
                            .users(dto.getUsers())
                            .createdAt(dto.getCreatedAt())
                            .lastMessageAt(dto.getLastMessageAt())
                            .isActive(dto.isActive())
                            .roomImageUrl(defaultImage.getOriginalImageUrl())
                            .roomThumbnailImageUrl(defaultImage.getThumbnailImageUrl())
                            .isOwner(dto.isOwner())
                            .inviteCode(dto.getInviteCode())
                            .inviteCodeExpiredAt(dto.getInviteCodeExpiredAt())
                            .totalPages(totalPages)
                            .build();
                } catch (Exception e) {
                    log.error("기본 그룹 이미지 설정 실패 - 채팅방 ID: {}, 에러: {}", chatRoom.getId(), e.getMessage(), e);
                    return dto;
                }
            }

            return dto;
        } catch (Exception e) {
            log.error("채팅방 응답 생성 실패 - 채팅방 ID: {}, 에러: {}", chatRoom.getId(), e.getMessage(), e);

            return ChatRoomResponseDto.builder()
                    .id(chatRoom.getId())
                    .name(chatRoom.getName())
                    .type(chatRoom.getType())
                    .owner(chatRoom.getOwner() != null ? ChatUserResponseDto.from(chatRoom.getOwner()) : null)
                    .users(List.of())
                    .createdAt(chatRoom.getCreatedAt())
                    .lastMessageAt(chatRoom.getLastMessageAt())
                    .isActive(chatRoom.isActive())
                    .roomImageUrl(chatRoom.getRoomImageUrl())
                    .roomThumbnailImageUrl(chatRoom.getRoomThumbnailImageUrl())
                    .isOwner(chatRoom.isUserOwner(currentUserId))
                    .inviteCode(chatRoom.getInviteCode())
                    .inviteCodeExpiredAt(chatRoom.getInviteCodeExpiredAt())
                    .totalPages(totalPages)
                    .build();
        }
    }

    private void handleOwnerLeaving(ChatRoom chatRoom) {
        chatRoom.deactivate();
        sendSystemMessage(chatRoom, "방장이 나가서 채팅방이 비활성화되었습니다. 더 이상 메시지를 보낼 수 없습니다.");
        log.info("채팅방 {}이 방장에 의해 비활성화되었습니다.", chatRoom.getId());
    }

    private void sendSystemMessage(ChatRoom chatRoom, String content) {
        ChatMessage systemMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(null)
                .content(content)
                .type(MessageType.SYSTEM)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(systemMessage);
        chatRoom.updateLastMessageAt();

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(savedMessage);
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId(), responseDto);
    }

    private void sendDirectMessage(User user, String content) {
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                content
        );
    }
}