package com.sonsminpark.auratalkback.domain.friend.service;


import com.sonsminpark.auratalkback.domain.friend.dto.response.FriendRequestResponseDto;
import com.sonsminpark.auratalkback.domain.friend.dto.response.FriendListResponseDto;
import com.sonsminpark.auratalkback.domain.friend.entity.FriendStatus;

import java.util.List;

public interface FriendService {

    FriendRequestResponseDto sendFriendRequest(Long requesterId, Long recipientId);

    void acceptFriendRequest(Long recipientId, Long requesterId);

    void removeFriendRequest(Long requesterId, Long recipientId);

    void deleteFriend(Long requesterId, Long recipientId);

    List<FriendListResponseDto> getSentFriendRequests(Long userId);

    List<FriendListResponseDto> getReceivedFriendRequests(Long userId);

    List<FriendListResponseDto> getFriends(Long userId);

    void blockFriend(Long userId, Long userId1);

    void unblockFriend(Long userId, Long userId1);

    List<FriendListResponseDto> getBlockedFriends(Long userId);

    FriendStatus getFriendStatus(Long currentUserId, Long targetUserId);

    List<FriendListResponseDto> searchFriends(Long userId, String keyword);

    List<FriendListResponseDto> searchUsers(Long userId, String keyword);
}
