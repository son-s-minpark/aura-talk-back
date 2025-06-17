package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInvitationResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatInvitation;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomUser;
import com.sonsminpark.auratalkback.domain.chat.entity.InvitationStatus;
import com.sonsminpark.auratalkback.domain.chat.exception.ChatInvitationException;
import com.sonsminpark.auratalkback.domain.chat.exception.ChatRoomNotFoundException;
import com.sonsminpark.auratalkback.domain.chat.exception.InvalidChatRoomStateException;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatInvitationRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.domain.user.service.UserProfileImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatInvitationService {

    private final ChatInvitationRepository chatInvitationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;
    private final UserProfileImageService userProfileImageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<ChatInvitationResponseDto> getPendingInvitations(Long userId) {
        validateUserExists(userId);

        List<ChatInvitation> invitations = chatInvitationRepository.findByInviteeIdAndStatus(
                userId, InvitationStatus.PENDING);

        return invitations.stream()
                .map(this::buildInvitationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatInvitationResponseDto> getSentInvitations(Long userId) {
        validateUserExists(userId);

        List<ChatInvitation> invitations = chatInvitationRepository.findByInviterId(userId);

        return invitations.stream()
                .map(this::buildInvitationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptInvitation(Long invitationId, Long userId) {
        ChatInvitation invitation = findInvitationById(invitationId);

        if (!invitation.getInvitee().getId().equals(userId)) {
            throw ChatInvitationException.notFound();
        }

        if (!invitation.isPending()) {
            throw ChatInvitationException.alreadyProcessed();
        }

        ChatRoom chatRoom = invitation.getChatRoom();
        if (!chatRoom.isActive()) {
            throw InvalidChatRoomStateException.deactivated();
        }

        if (isUserInChatRoom(chatRoom.getId(), userId)) {
            throw InvalidChatRoomStateException.alreadyMember();
        }

        invitation.accept();

        addUserToChatRoom(chatRoom, invitation.getInvitee());

        String message = invitation.getInvitee().getNickname() + "님이 초대를 수락하여 입장했습니다.";
        sendNotificationToRoom(chatRoom.getId(), message);

        sendDirectNotification(invitation.getInviter().getId(),
                invitation.getInvitee().getNickname() + "님이 '" + chatRoom.getName() + "' 채팅방 초대를 수락했습니다.");

        log.info("채팅방 초대 수락 완료 - 초대 ID: {}, 사용자: {}, 채팅방: {}",
                invitationId, userId, chatRoom.getId());
    }

    @Transactional
    public void rejectInvitation(Long invitationId, Long userId) {
        ChatInvitation invitation = findInvitationById(invitationId);

        if (!invitation.getInvitee().getId().equals(userId)) {
            throw ChatInvitationException.notFound();
        }

        if (!invitation.isPending()) {
            throw ChatInvitationException.alreadyProcessed();
        }

        invitation.reject();

        sendDirectNotification(invitation.getInviter().getId(),
                invitation.getInvitee().getNickname() + "님이 '" + invitation.getChatRoom().getName() + "' 채팅방 초대를 거절했습니다.");

        log.info("채팅방 초대 거절 완료 - 초대 ID: {}, 사용자: {}", invitationId, userId);
    }

    @Transactional
    public void deleteInvitation(Long invitationId, Long userId) {
        ChatInvitation invitation = findInvitationById(invitationId);

        if (!invitation.getInviter().getId().equals(userId) &&
                !invitation.getInvitee().getId().equals(userId)) {
            throw ChatInvitationException.notFound();
        }

        chatInvitationRepository.delete(invitation);

        log.info("초대 삭제 완료 - 초대 ID: {}, 사용자: {}", invitationId, userId);
    }

    @Transactional
    public ChatInvitation createInvitation(Long chatRoomId, Long inviterId, Long inviteeId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        User inviter = findUserById(inviterId);
        User invitee = findUserById(inviteeId);

        chatInvitationRepository.findByChatRoomIdAndInviteeIdAndStatus(
                        chatRoomId, inviteeId, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw ChatInvitationException.duplicateInvitation();
                });

        if (isUserInChatRoom(chatRoomId, inviteeId)) {
            throw InvalidChatRoomStateException.alreadyMember();
        }

        ChatInvitation invitation = ChatInvitation.builder()
                .chatRoom(chatRoom)
                .inviter(inviter)
                .invitee(invitee)
                .status(InvitationStatus.PENDING)
                .build();

        ChatInvitation savedInvitation = chatInvitationRepository.save(invitation);

        sendDirectNotification(inviteeId,
                inviter.getNickname() + "님이 '" + chatRoom.getName() + "' 채팅방에 초대했습니다.");

        log.info("채팅방 초대 생성 완료 - 채팅방: {}, 초대자: {}, 피초대자: {}",
                chatRoomId, inviterId, inviteeId);

        return savedInvitation;
    }

    private ChatInvitation findInvitationById(Long invitationId) {
        return chatInvitationRepository.findById(invitationId)
                .orElseThrow(() -> ChatInvitationException.notFound());
    }

    private ChatRoom findChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatRoomNotFoundException.of(chatRoomId));
    }

    private User findUserById(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException.of(userId);
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

    private ChatInvitationResponseDto buildInvitationResponse(ChatInvitation invitation) {
        ChatInvitationResponseDto dto = ChatInvitationResponseDto.from(invitation);

        try {
            String inviterThumbnailUrl = userProfileImageService.getProfileImage(
                    invitation.getInviter().getId()).getThumbnailImageUrl();
            dto.setInviterThumbnailUrl(inviterThumbnailUrl);

            String inviteeThumbnailUrl = userProfileImageService.getProfileImage(
                    invitation.getInvitee().getId()).getThumbnailImageUrl();
            dto.setInviteeThumbnailUrl(inviteeThumbnailUrl);
        } catch (Exception e) {
            log.warn("프로필 이미지 조회 실패 - 초대 ID: {}, 오류: {}", invitation.getId(), e.getMessage());
        }

        return dto;
    }

    private void sendNotificationToRoom(Long chatRoomId, String message) {
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId, message);
    }

    private void sendDirectNotification(Long userId, String message) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                message
        );
    }
}