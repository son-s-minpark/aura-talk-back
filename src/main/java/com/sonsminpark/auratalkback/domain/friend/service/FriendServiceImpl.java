package com.sonsminpark.auratalkback.domain.friend.service;

import com.sonsminpark.auratalkback.domain.friend.dto.response.FriendListResponseDto;
import com.sonsminpark.auratalkback.domain.friend.dto.response.FriendRequestResponseDto;
import com.sonsminpark.auratalkback.domain.friend.entity.Friend;
import com.sonsminpark.auratalkback.domain.friend.entity.FriendBlock;
import com.sonsminpark.auratalkback.domain.friend.entity.FriendRequest;
import com.sonsminpark.auratalkback.domain.friend.entity.FriendStatus;
import com.sonsminpark.auratalkback.domain.friend.exception.*;
import com.sonsminpark.auratalkback.domain.friend.repository.FriendBlockRepository;
import com.sonsminpark.auratalkback.domain.friend.repository.FriendRepository;
import com.sonsminpark.auratalkback.domain.friend.repository.FriendRequestRepository;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendBlockRepository friendBlockRepository;

    private User getUserById(Long userId) {
        return userRepository.findByIdWithProfileImage(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));
    }

    @Override
    @Transactional
    public FriendRequestResponseDto sendFriendRequest(Long requesterId, Long recipientId) {

        if (requesterId.equals(recipientId)) {
            throw SelfFriendRequestException.create();
        }

        User requester = getUserById(requesterId);
        User recipient = getUserById(recipientId);

        friendRepository.findByUsers(requester, recipient)
                .ifPresent(friend -> {
                    throw AlreadyFriendException.between(requesterId, recipientId);
                });

        friendRequestRepository.findByRequesterAndRecipient(requester, recipient)
                .ifPresent(friendRequest -> {
                    throw DuplicateFriendRequestException.between(requesterId, recipientId);
                });

        friendBlockRepository.findByBlockerAndBlocked(requester, recipient)
                .ifPresent(friendBlock -> {
                    throw BlockedUserFriendRequestException.create();
                });

        Optional<FriendRequest> recipientRequest = friendRequestRepository.findByRequesterAndRecipient(recipient, requester);
        if (recipientRequest.isPresent()) {

            friendRepository.save(Friend.create(requester, recipient));
            friendRequestRepository.delete(recipientRequest.get());

            return FriendRequestResponseDto.builder()
                    .status(FriendStatus.FRIENDS)
                    .requesterId(requesterId)
                    .recipientId(recipientId)
                    .build();

        } else {

            FriendRequest friendRequest = FriendRequest.builder()
                    .requester(requester)
                    .recipient(recipient)
                    .build();
            friendRequestRepository.save(friendRequest);

            return FriendRequestResponseDto.builder()
                    .status(FriendStatus.REQUEST_SENT)
                    .requesterId(requesterId)
                    .recipientId(recipientId)
                    .build();
        }
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long recipientId, Long requesterId) {

        User requester = getUserById(requesterId);
        User recipient = getUserById(recipientId);

        FriendRequest friendRequest = friendRequestRepository.findByRequesterAndRecipient(requester, recipient)
                .orElseThrow(() -> FriendRequestNotFoundException.between(requesterId, recipientId));

        friendRepository.save(Friend.create(requester, recipient));
        friendRequestRepository.delete(friendRequest);
    }

    @Override
    @Transactional
    public void removeFriendRequest(Long requesterId, Long recipientId) {

        User requester = getUserById(requesterId);
        User recipient = getUserById(recipientId);

        FriendRequest friendRequest = friendRequestRepository.findByRequesterAndRecipient(requester, recipient)
                .orElseThrow(() -> FriendRequestNotFoundException.between(requesterId, recipientId));

        friendRequestRepository.delete(friendRequest);
    }

    @Override
    public List<FriendListResponseDto> getSentFriendRequests(Long userId) {

        User user = getUserById(userId);

        List<FriendRequest> friendRequests = friendRequestRepository.findByRequester(user);

        return friendRequests.stream()
                .map(friendRequest -> FriendListResponseDto.from(friendRequest.getRecipient(), FriendStatus.REQUEST_SENT))
                .toList();
    }

    @Override
    public List<FriendListResponseDto> getReceivedFriendRequests(Long userId) {

        User user = getUserById(userId);

        List<FriendRequest> friendRequests = friendRequestRepository.findByRecipientExcludingBlocked(user);

        return friendRequests.stream()
                .map(friendRequest -> FriendListResponseDto.from(friendRequest.getRequester(), FriendStatus.REQUEST_RECEIVED))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendListResponseDto> getFriends(Long userId) {

        getUserById(userId);

        List<User> friends = friendRepository.findFriendUsers(userId);

        return friends.stream()
                .map(friend -> FriendListResponseDto.from(friend, FriendStatus.FRIENDS))
                .toList();
    }

    @Override
    public List<FriendListResponseDto> getBlockedFriends(Long userId) {

        getUserById(userId);

        List<User> blockedUsers = friendBlockRepository.findBlockedUsers(userId);

        return blockedUsers.stream()
                .map(user -> FriendListResponseDto.from(user, FriendStatus.BLOCKED))
                .toList();
    }

    @Override
    @Transactional
    public void blockFriend(Long requesterId, Long recipientId) {

        User requester = getUserById(requesterId);
        User recipient = getUserById(recipientId);

        friendRepository.findByUsers(requester, recipient)
                .ifPresent(friendRepository::delete);

        friendRequestRepository.findByRequesterAndRecipient(requester, recipient)
                .ifPresent(friendRequestRepository::delete);
        friendRequestRepository.findByRequesterAndRecipient(recipient, requester)
                .ifPresent(friendRequestRepository::delete);

        friendBlockRepository.findByBlockerAndBlocked(requester, recipient)
                .orElseGet(() -> friendBlockRepository.save(FriendBlock.builder()
                        .blocker(requester)
                        .blocked(recipient)
                        .build()));
    }

    @Override
    public void unblockFriend(Long requesterId, Long recipientId) {

        User requester = getUserById(requesterId);
        User recipient = getUserById(recipientId);

        FriendBlock friendBlock = friendBlockRepository.findByBlockerAndBlocked(requester, recipient)
                .orElseThrow(() -> FriendBlockNotFoundException.between(requesterId, recipientId));

        friendBlockRepository.delete(friendBlock);

        friendRequestRepository.findByRequesterAndRecipient(recipient, requester)
                .ifPresent(friendRequestRepository::delete);
    }

    @Override
    @Transactional
    public void deleteFriend(Long requesterId, Long recipientId) {

        User requester = getUserById(requesterId);
        User recipient = getUserById(recipientId);

        Friend friend = friendRepository.findByUsers(requester, recipient)
                .orElseThrow(() -> FriendNotFoundException.between(requesterId, recipientId));

        friendRepository.delete(friend);
    }

    @Override
    public FriendStatus getFriendStatus(Long currentUserId, Long targetUserId) {

        User currentUser = getUserById(currentUserId);
        User targetUser = getUserById(targetUserId);

        if (friendRepository.findByUsers(currentUser, targetUser).isPresent())
            return FriendStatus.FRIENDS;

        if (friendBlockRepository.findByBlockerAndBlocked(currentUser, targetUser).isPresent())
            return FriendStatus.BLOCKED;

        if (friendRequestRepository.findByRequesterAndRecipient(currentUser, targetUser).isPresent())
            return FriendStatus.REQUEST_SENT;

        if (friendRequestRepository.findByRequesterAndRecipient(targetUser, currentUser).isPresent())
            return FriendStatus.REQUEST_RECEIVED;

        return FriendStatus.NONE;
    }
}
