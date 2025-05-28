package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInviteResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInvitationResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.*;
import com.sonsminpark.auratalkback.domain.chat.exception.*;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatInvitationRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatMessageRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.domain.user.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto, Long userId) {
        User owner = findUserById(userId);

        ChatRoom chatRoom = ChatRoom.builder()
                .name(requestDto.getName())
                .type(ChatRoomType.GROUP)
                .owner(owner)
                .isActive(true)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 방장을 채팅방에 추가
        addUserToChatRoom(savedChatRoom, owner);

        if (requestDto.getUserIds() != null && !requestDto.getUserIds().isEmpty()) {
            processInitialInvitations(savedChatRoom, owner, requestDto.getUserIds());
        }

        return buildChatRoomResponse(savedChatRoom, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponseDto> getChatRoomsByUserId(Long userId) {
        validateUserExists(userId);

        List<ChatRoom> chatRooms = chatRoomRepository.findActiveByUserId(userId);

        return chatRooms.stream()
                .map(chatRoom -> buildChatRoomResponse(chatRoom, userId))
                .collect(Collectors.toList());
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

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(savedMessage);

        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId, responseDto);

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponseDto> getMessages(Long chatRoomId, Long userId, Pageable pageable) {
        validateChatRoomAccess(chatRoomId, userId);

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoomId, pageable);
        return messages.map(ChatMessageResponseDto::from);
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findByIdAndSenderId(messageId, userId)
                .orElseThrow(() -> MessageNotFoundException.of(messageId));

        message.softDelete();

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(message);
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
    public ChatInvitationResponseDto inviteUser(Long chatRoomId, ChatInviteRequestDto requestDto, Long userId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        User inviter = findUserById(userId);
        User invitee = findUserById(requestDto.getUserId());

        validateChatRoomAccess(chatRoomId, userId);
        validateChatRoomActive(chatRoom);

        // 이미 참여중인 사용자 확인
        if (isUserInChatRoom(chatRoomId, invitee.getId())) {
            throw InvalidChatRoomStateException.alreadyMember();
        }

        validateNoDuplicateInvitation(chatRoomId, invitee.getId());

        ChatInvitation invitation = ChatInvitation.builder()
                .chatRoom(chatRoom)
                .inviter(inviter)
                .invitee(invitee)
                .status(InvitationStatus.PENDING)
                .build();

        ChatInvitation savedInvitation = chatInvitationRepository.save(invitation);

        sendInvitationMessage(chatRoom, inviter, invitee);

        log.info("사용자 {}가 사용자 {}를 채팅방 {}에 초대했습니다.", userId, requestDto.getUserId(), chatRoomId);

        return ChatInvitationResponseDto.from(savedInvitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatInvitationResponseDto> getPendingInvitations(Long userId) {
        validateUserExists(userId);

        List<ChatInvitation> invitations = chatInvitationRepository.findByInviteeIdAndStatus(
                userId, InvitationStatus.PENDING);

        return invitations.stream()
                .map(ChatInvitationResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void acceptInvitation(Long invitationId, Long userId) {
        ChatInvitation invitation = findInvitationById(invitationId);

        validateInvitationAccess(invitation, userId);
        validateInvitationPending(invitation);

        ChatRoom chatRoom = invitation.getChatRoom();
        User invitee = invitation.getInvitee();

        validateChatRoomActive(chatRoom);

        if (isUserInChatRoom(chatRoom.getId(), invitee.getId())) {
            invitation.accept();
            throw InvalidChatRoomStateException.alreadyMember();
        }

        invitation.accept();
        addUserToChatRoom(chatRoom, invitee);

        sendSystemMessage(chatRoom, invitee.getNickname() + "님이 초대를 수락하고 입장했습니다.");
        notifyOwnerInvitationAccepted(chatRoom.getOwner(), invitee, chatRoom);

        log.info("사용자 {}가 채팅방 {} 초대를 수락했습니다.", userId, chatRoom.getId());
    }

    @Override
    @Transactional
    public void rejectInvitation(Long invitationId, Long userId) {
        ChatInvitation invitation = findInvitationById(invitationId);

        validateInvitationAccess(invitation, userId);
        validateInvitationPending(invitation);

        invitation.reject();

        notifyOwnerInvitationRejected(invitation.getChatRoom().getOwner(), invitation.getInvitee(), invitation.getChatRoom());

        log.info("사용자 {}가 채팅방 {} 초대를 거절했습니다.", userId, invitation.getChatRoom().getId());
    }

    @Override
    @Transactional
    public void acceptInvite(String inviteCode, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode)
                .orElseThrow(() -> ChatInvitationException.invalidInviteCode());

        if (!chatRoom.isInviteCodeValid()) {
            throw ChatInvitationException.expiredInvite();
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
    public void rejectInvite(String inviteCode, Long userId) {
        log.info("사용자 {}가 초대 코드 {}를 거절했습니다.", userId, inviteCode);
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

    private ChatInvitation findInvitationById(Long invitationId) {
        return chatInvitationRepository.findById(invitationId)
                .orElseThrow(() -> ChatInvitationException.notFound());
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
        if (!isUserInChatRoom(chatRoomId, userId)) {
            throw ChatAccessDeniedException.of("채팅방에 참여할 권한이 없습니다.");
        }
    }

    private void validateOwnerPermission(ChatRoom chatRoom, Long userId) {
        if (!chatRoom.isUserOwner(userId)) {
            throw InvalidChatRoomStateException.ownerRequired();
        }
    }

    private void validateInvitationAccess(ChatInvitation invitation, Long userId) {
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw ChatAccessDeniedException.of("초대에 대한 권한이 없습니다.");
        }
    }

    private void validateInvitationPending(ChatInvitation invitation) {
        if (!invitation.isPending()) {
            throw ChatInvitationException.alreadyProcessed();
        }
    }

    private void validateNoDuplicateInvitation(Long chatRoomId, Long inviteeId) {
        chatInvitationRepository.findByChatRoomIdAndInviteeIdAndStatus(
                chatRoomId, inviteeId, InvitationStatus.PENDING
        ).ifPresent(invitation -> {
            throw ChatInvitationException.duplicateInvitation();
        });
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

    private void processInitialInvitations(ChatRoom chatRoom, User owner, List<Long> userIds) {
        List<User> invitedUsers = userRepository.findAllById(userIds);

        for (User invitedUser : invitedUsers) {
            try {
                ChatInvitation invitation = ChatInvitation.builder()
                        .chatRoom(chatRoom)
                        .inviter(owner)
                        .invitee(invitedUser)
                        .status(InvitationStatus.PENDING)
                        .build();
                chatInvitationRepository.save(invitation);

                sendInvitationMessage(chatRoom, owner, invitedUser);
            } catch (Exception e) {
                log.warn("초대 처리 중 오류 발생 - 사용자: {}, 오류: {}", invitedUser.getId(), e.getMessage());
            }
        }
    }

    private ChatRoomResponseDto buildChatRoomResponse(ChatRoom chatRoom, Long currentUserId) {
        ChatRoomResponseDto dto = ChatRoomResponseDto.from(chatRoom, currentUserId);

        List<ChatRoomUser> roomUsers = chatRoomUserRepository.findAllByChatRoomId(chatRoom.getId());
        List<UserResponseDto> users = roomUsers.stream()
                .map(roomUser -> UserResponseDto.from(roomUser.getUser()))
                .collect(Collectors.toList());

        dto.setUsers(users);
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

    private void sendInvitationMessage(ChatRoom chatRoom, User inviter, User invitee) {
        String content = inviter.getNickname() + "님이 '" + chatRoom.getName() + "' 채팅방에 초대했습니다.";
        sendDirectMessage(invitee, content);
    }

    private void sendDirectMessage(User user, String content) {
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                content
        );
    }

    private void notifyOwnerInvitationAccepted(User owner, User invitee, ChatRoom chatRoom) {
        if (owner != null) {
            sendDirectMessage(owner,
                    invitee.getNickname() + "님이 " + chatRoom.getName() + " 채팅방 초대를 수락했습니다.");
        }
    }

    private void notifyOwnerInvitationRejected(User owner, User invitee, ChatRoom chatRoom) {
        if (owner != null) {
            sendDirectMessage(owner,
                    invitee.getNickname() + "님이 " + chatRoom.getName() + " 채팅방 초대를 거절했습니다.");
        }
    }
}