package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInviteResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.*;
import com.sonsminpark.auratalkback.domain.chat.exception.ChatAccessDeniedException;
import com.sonsminpark.auratalkback.domain.chat.exception.ChatRoomNotFoundException;
import com.sonsminpark.auratalkback.domain.chat.exception.MessageNotFoundException;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatMessageRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto, Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        ChatRoom chatRoom = ChatRoom.builder()
                .name(requestDto.getName())
                .type(ChatRoomType.GROUP)
                .owner(owner)
                .isActive(true)
                .build();

        chatRoom.addUser(owner);

        if (requestDto.getUserIds() != null && !requestDto.getUserIds().isEmpty()) {
            List<User> invitedUsers = userRepository.findAllById(requestDto.getUserIds());
            invitedUsers.forEach(chatRoom::addUser);
        }

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        ChatRoomUser roomUser = ChatRoomUser.builder()
                .chatRoom(savedChatRoom)
                .user(owner)
                .notificationEnabled(true)
                .build();
        chatRoomUserRepository.save(roomUser);

        return ChatRoomResponseDto.from(savedChatRoom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponseDto> getChatRoomsByUserId(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findActiveByUserId(userId);
        return chatRooms.stream()
                .map(ChatRoomResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void leaveChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        if (!chatRoom.getUsers().contains(user)) {
            throw ChatAccessDeniedException.of("채팅방에 참여하고 있지 않습니다.");
        }

        // 방장이 나가는 경우, 채팅방 비활성화
        if (chatRoom.getOwner() != null && chatRoom.getOwner().getId().equals(userId)) {
            chatRoom.deactivate();
        } else {
            chatRoom.removeUser(user);
        }

        ChatRoomUser roomUser = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElse(null);
        if (roomUser != null) {
            chatRoomUserRepository.delete(roomUser);
        }

        sendSystemMessage(chatRoom, user.getNickname() + "님이 채팅방을 나갔습니다.");
    }

    @Override
    @Transactional
    public ChatMessageResponseDto sendMessage(Long chatRoomId, ChatMessageRequestDto requestDto, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        if (!chatRoomRepository.isUserInChatRoom(chatRoomId, userId)) {
            throw ChatAccessDeniedException.of("채팅방에 참여할 권한이 없습니다.");
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(requestDto.getContent())
                .type(requestDto.getType())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        chatRoom.updateLastMessageAt();

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(savedMessage);

        // WebSocket으로 실시간 메시지 전송
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId, responseDto);

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponseDto> getMessages(Long chatRoomId, Long userId, Pageable pageable) {
        if (!chatRoomRepository.isUserInChatRoom(chatRoomId, userId)) {
            throw ChatAccessDeniedException.of("채팅방에 참여할 권한이 없습니다.");
        }

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoomId, pageable);
        return messages.map(ChatMessageResponseDto::from);
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findByIdAndSenderId(messageId, userId)
                .orElseThrow(() -> MessageNotFoundException.of(messageId));

        message.softDelete();
    }

    @Override
    @Transactional
    public ChatInviteResponseDto createInviteLink(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));

        if (!chatRoom.getOwner().getId().equals(userId)) {
            throw ChatAccessDeniedException.of("방장만 초대 링크를 생성할 수 있습니다.");
        }

        String inviteCode = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        chatRoom.generateInviteCode(inviteCode, expiresAt);

        String inviteLink = "https://auratalk.com/invite/" + inviteCode;

        return ChatInviteResponseDto.builder()
                .inviteCode(inviteCode)
                .inviteLink(inviteLink)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    @Transactional
    public void inviteUser(Long chatRoomId, ChatInviteRequestDto requestDto, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));

        if (!chatRoomRepository.isUserInChatRoom(chatRoomId, userId)) {
            throw ChatAccessDeniedException.of("채팅방에 참여할 권한이 없습니다.");
        }

        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        User invitedUser = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> UserNotFoundException.of(requestDto.getUserId()));

        if (chatRoom.getUsers().contains(invitedUser)) {
            throw new IllegalStateException("이미 채팅방에 참여중인 사용자입니다.");
        }

        // 초대 링크 생성
        String inviteCode = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        chatRoom.generateInviteCode(inviteCode, expiresAt);

        String inviteLink = "https://auratalk.com/invite/" + inviteCode;
        sendSystemMessage(chatRoom, inviter.getNickname() + "님이 " + invitedUser.getNickname() + "님을 초대했습니다.");

        // TODO: 초대받은 사용자에게 알림 전송
    }

    @Override
    @Transactional
    public void acceptInvite(String inviteCode, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));

        if (!chatRoom.isInviteCodeValid()) {
            throw new IllegalArgumentException("만료된 초대 링크입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        if (chatRoom.getUsers().contains(user)) {
            throw new IllegalStateException("이미 채팅방에 참여중입니다.");
        }

        chatRoom.addUser(user);

        ChatRoomUser roomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .notificationEnabled(true)
                .build();
        chatRoomUserRepository.save(roomUser);

        sendSystemMessage(chatRoom, user.getNickname() + "님이 채팅방에 참여했습니다.");
    }

    @Override
    @Transactional
    public void rejectInvite(String inviteCode, Long userId) {
        log.info("User {} rejected invite with code: {}", userId, inviteCode);
    }

    @Override
    @Transactional
    public void updateNotificationSettings(Long chatRoomId, Long userId, boolean enabled) {
        ChatRoomUser roomUser = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 설정을 찾을 수 없습니다."));

        roomUser.updateNotificationSetting(enabled);
    }

    private void sendSystemMessage(ChatRoom chatRoom, String content) {
        ChatMessage systemMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(null)
                .content(content)
                .type(MessageType.SYSTEM)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(systemMessage);

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(savedMessage);
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId(), responseDto);
    }
}