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

    @Override
    @Transactional
    public FriendRequestResponseDto sendFriendRequest(Long requesterId, Long recipientId) {

        if (requesterId.equals(recipientId)) {
            throw SelfFriendRequestException.create();
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> UserNotFoundException.of(requesterId));

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> UserNotFoundException.of(recipientId));

        Optional<Friend> existingFriend = friendRepository.findByUsers(requester, recipient);
        if (existingFriend.isPresent()) {
            throw AlreadyFriendException.between(requesterId, recipientId);
        }

        Optional<FriendRequest> existingFriendRequest = friendRequestRepository.findByRequesterAndRecipient(requester, recipient);
        if (existingFriendRequest.isPresent()) {
            throw DuplicateFriendRequestException.between(requesterId, recipientId);
        }

        Optional<FriendBlock> existingFriendBlock = friendBlockRepository.findByBlockerAndBlocked(requester, recipient);
        if (existingFriendBlock.isPresent()) {
            throw BlockedUserFriendRequestException.create();
        }

        Optional<FriendRequest> recipientRequest = friendRequestRepository.findByRequesterAndRecipient(recipient, requester);
        if (recipientRequest.isPresent()) {

            Friend friend = Friend.create(requester, recipient);
            friendRepository.save(friend);

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

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> UserNotFoundException.of(requesterId));

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> UserNotFoundException.of(recipientId));

        FriendRequest friendRequest = friendRequestRepository.findByRequesterAndRecipient(requester, recipient)
                .orElseThrow(() -> FriendRequestNotFoundException.between(requesterId, recipientId));

        Friend friend = Friend.create(requester, recipient);
        friendRepository.save(friend);
        friendRequestRepository.delete(friendRequest);

    }

    @Override
    @Transactional
    public void removeFriendRequest(Long requesterId, Long recipientId) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> UserNotFoundException.of(requesterId));

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> UserNotFoundException.of(recipientId));

        FriendRequest friendRequest = friendRequestRepository.findByRequesterAndRecipient(requester, recipient)
                .orElseThrow(() -> FriendRequestNotFoundException.between(requesterId, recipientId));

        friendRequestRepository.delete(friendRequest);
    }

    @Override
    public List<FriendListResponseDto> getSentFriendRequests(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        List<FriendRequest> friendRequests = friendRequestRepository.findByRequester(user);

        return friendRequests.stream()
                .map(friendRequest -> FriendListResponseDto.from(friendRequest.getRecipient(), FriendStatus.REQUEST_SENT))
                .toList();
    }

    @Override
    public List<FriendListResponseDto> getReceivedFriendRequests(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        List<FriendRequest> friendRequests = friendRequestRepository.findByRecipientExcludingBlocked(user);


        return friendRequests.stream()
                .map(friendRequest -> FriendListResponseDto.from(friendRequest.getRequester(), FriendStatus.REQUEST_RECEIVED))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendListResponseDto> getFriends(Long userId) {

        List<User> friends = friendRepository.findFriendUsers(userId);

        return friends.stream()
                .map(friend -> FriendListResponseDto.from(friend, FriendStatus.FRIENDS))
                .toList();
    }

    @Override
    public List<FriendListResponseDto> getBlockedFriends(Long userId) {

        List<User> blockedUsers = friendBlockRepository.findBlockedUsers(userId);

        return blockedUsers.stream()
                .map(user -> FriendListResponseDto.from(user, FriendStatus.BLOCKED))
                .toList();
    }

    @Override
    @Transactional
    public void blockFriend(Long requesterId, Long recipientId) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> UserNotFoundException.of(requesterId));

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> UserNotFoundException.of(recipientId));


        Optional<Friend> existingFriend = friendRepository.findByUsers(requester, recipient);

        existingFriend.ifPresent(friendRepository::delete);

        FriendBlock friendBlock = FriendBlock.builder()
                .blocker(requester)
                .blocked(recipient)
                .build();
        friendBlockRepository.save(friendBlock);
    }

    @Override
    public void unblockFriend(Long requesterId, Long recipientId) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> UserNotFoundException.of(requesterId));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> UserNotFoundException.of(recipientId));

        FriendBlock friendBlock = friendBlockRepository.findByBlockerAndBlocked(requester, recipient)
                .orElseThrow(() -> FriendBlockNotFoundException.between(requesterId, recipientId));

        friendBlockRepository.delete(friendBlock);

        Optional<FriendRequest> existingFriendRequest = friendRequestRepository.findByRequesterAndRecipient(recipient, requester);
        existingFriendRequest.ifPresent(friendRequestRepository::delete);
    }

    @Override
    @Transactional
    public void deleteFriend(Long requesterId, Long recipientId) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> UserNotFoundException.of(requesterId));

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> UserNotFoundException.of(recipientId));

        Friend friend = friendRepository.findByUsers(requester, recipient)
                .orElseThrow(() -> FriendNotFoundException.between(requesterId, recipientId));

        friendRepository.delete(friend);
    }

    @Override
    public FriendStatus getFriendStatus(Long currentUserId, Long targetUserId) {

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> UserNotFoundException.of(currentUserId));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> UserNotFoundException.of(targetUserId));

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
