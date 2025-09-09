package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomUpdateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {
    // 채팅방
    ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto, Long userId);

    ChatRoomResponseDto createOneToOneChatRoom(Long userId, Long targetUserId);

    List<ChatRoomResponseDto> getChatRoomsByUserId(Long userId);

    Page<ChatRoomResponseDto> getChatRoomsByUserId(Long userId, int page, int size);

    ChatRoomResponseDto getChatRoomInfo(Long chatRoomId, Long userId);

    ChatRoomResponseDto updateChatRoom(Long chatRoomId, ChatRoomUpdateRequestDto requestDto, Long userId);

    ChatRoomResponseDto deleteRoomImage(Long chatRoomId, Long userId);

    void leaveChatRoom(Long chatRoomId, Long userId);

    void deleteChatRoom(Long chatRoomId, Long userId);

    void kickUser(Long chatRoomId, Long ownerId, Long targetUserId);

    void unbanUser(Long chatRoomId, Long ownerId, Long targetUserId);

    List<ChatUserResponseDto> getBannedUsers(Long chatRoomId, Long userId);

    List<ChatRoomResponseDto> searchChatRooms(String keyword, Long userId);

    // 메시지
    ChatMessageResponseDto sendMessage(Long chatRoomId, ChatMessageRequestDto requestDto, Long userId);

    ChatMessagesResponseDto getMessagesBefore(Long chatRoomId, Long userId, Long beforeMessageId, int limit);

    ChatMessagesResponseDto getMessagesAfter(Long chatRoomId, Long userId, Long afterMessageId, int limit);

    void deleteMessage(Long messageId, Long userId);

    @Deprecated
    Page<ChatMessageResponseDto> getMessages(Long chatRoomId, Long userId, Pageable pageable);

    // 초대 링크
    ChatInviteResponseDto createInviteLink(Long chatRoomId, Long userId);

    ChatInviteResponseDto sendInviteToFriend(Long chatRoomId, ChatInviteRequestDto requestDto, Long userId);

    void acceptInvite(String inviteCode, Long userId);

    // 설정
    void updateNotificationSettings(Long chatRoomId, Long userId, boolean enabled);
}