package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomUpdateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInviteResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatUserResponseDto;
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
import org.springframework.data.domain.Pageable;
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

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final int DEFAULT_GROUP_IMAGE_COUNT = 2;

    private record DefaultGroupImage(String originalUrl, String thumbnailUrl) {
    }

    private DefaultGroupImage getDefaultGroupImage(Long chatRoomId) {
        try {
            int index = (int) ((chatRoomId - 1) % DEFAULT_GROUP_IMAGE_COUNT) + 1;
            String prefix = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/";

            log.debug("기본 그룹 이미지 생성 - 채팅방 ID: {}, 계산된 인덱스: {}", chatRoomId, index);

            return new DefaultGroupImage(
                    prefix + index + ".png",
                    prefix + index + "_thumb.png"
            );
        } catch (Exception e) {
            log.error("기본 그룹 이미지 생성 실패 - 채팅방 ID: {}, 에러: {}", chatRoomId, e.getMessage());

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
        if (roomImageUrl != null && !roomImageUrl.trim().isEmpty()) {
            log.info("채팅방 이미지 설정 - URL: {}", roomImageUrl);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .name(requestDto.getName())
                .type(ChatRoomType.GROUP)
                .owner(owner)
                .roomImageUrl(roomImageUrl)
                .isActive(true)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 기본 이미지 설정 (사용자가 이미지를 제공하지 않은 경우)
        if (roomImageUrl == null || roomImageUrl.trim().isEmpty()) {
            try {
                DefaultGroupImage defaultImage = getDefaultGroupImage(savedChatRoom.getId());
                savedChatRoom.updateRoomImage(defaultImage.originalUrl());
                log.info("채팅방 기본 이미지 설정 완료 - ID: {}, 이미지 URL: {}",
                        savedChatRoom.getId(), defaultImage.originalUrl());
            } catch (Exception e) {
                log.error("채팅방 기본 이미지 설정 실패 - ID: {}, 에러: {}", savedChatRoom.getId(), e.getMessage());
                String fallbackUrl = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/1.png";
                savedChatRoom.updateRoomImage(fallbackUrl);
                log.warn("채팅방 기본 이미지를 fallback으로 설정 - ID: {}, URL: {}",
                        savedChatRoom.getId(), fallbackUrl);
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
    public ChatRoomResponseDto getChatRoomInfo(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithOwner(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));

        validateChatRoomAccess(chatRoomId, userId);

        return buildChatRoomResponse(chatRoom, userId);
    }

    @Override
    @Transactional
    public ChatRoomResponseDto updateChatRoom(Long chatRoomId, ChatRoomUpdateRequestDto requestDto, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);

        validateOwnerPermission(chatRoom, userId);
        validateChatRoomActive(chatRoom);

        if (requestDto.getName() != null && !requestDto.getName().trim().isEmpty()) {
            chatRoom.updateName(requestDto.getName().trim());
            sendSystemMessage(chatRoom, "채팅방 이름이 '" + requestDto.getName() + "'로 변경되었습니다.");
        }

        if (requestDto.getRoomImageUrl() != null) {
            chatRoom.updateRoomImage(requestDto.getRoomImageUrl());
            sendSystemMessage(chatRoom, "채팅방 이미지가 변경되었습니다.");
        }

        log.info("채팅방 정보 수정 완료 - ID: {}, 수정자: {}", chatRoomId, userId);
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

        ChatRoomUser roomUser = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                .orElseThrow(() -> InvalidChatRoomStateException.notMember());

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
        sendSystemMessage(chatRoom, targetUser.getNickname() + "님의 강퇴가 해제되었습니다.");

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
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId, responseDto);

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

        validateOwnerPermission(chatRoom, userId);
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
        sendSystemMessage(chatRoom, user.getNickname() + "님이 초대 링크를 통해 입장했습니다.");

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
        return chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> InvalidChatRoomStateException.notMember());
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
        return chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId).isPresent();
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

        // 그룹 채팅방인 경우 기본 이미지 설정
        if (chatRoom.getType() == ChatRoomType.GROUP &&
                (chatRoom.getRoomImageUrl() == null || chatRoom.getRoomImageUrl().isEmpty())) {
            DefaultGroupImage defaultImage = getDefaultGroupImage(chatRoom.getId());

            return ChatRoomResponseDto.builder()
                    .id(dto.getId())
                    .name(dto.getName())
                    .type(dto.getType())
                    .owner(dto.getOwner())
                    .users(dto.getUsers())
                    .createdAt(dto.getCreatedAt())
                    .lastMessageAt(dto.getLastMessageAt())
                    .isActive(dto.isActive())
                    .roomImageUrl(defaultImage.originalUrl())
                    .isOwner(dto.isOwner())
                    .inviteCode(dto.getInviteCode())
                    .inviteCodeExpiredAt(dto.getInviteCodeExpiredAt())
                    .build();
        }

        return dto;
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