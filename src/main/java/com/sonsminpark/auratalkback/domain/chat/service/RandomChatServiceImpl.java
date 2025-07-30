package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.RandomChatStartRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatUserResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.RandomChatMatchResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomUser;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.friend.repository.FriendBlockRepository;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.domain.user.service.UserProfileImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RandomChatServiceImpl implements RandomChatService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final FriendBlockRepository friendBlockRepository;
    private final UserProfileImageService userProfileImageService;

    @Override
    @Transactional
    public RandomChatMatchResponseDto startRandomChat(Long userId, RandomChatStartRequestDto requestDto) {
        User user = findUserById(userId);

        if (!user.isRandomChatEnabled()) {
            return createFailureResponse("랜덤 채팅이 비활성화되어 있습니다. 설정에서 활성화해주세요.");
        }

        List<String> userInterests = getUserInterests(user, requestDto);
        if (userInterests.isEmpty()) {
            return createFailureResponse("관심사를 설정해주세요.");
        }

        try {
            User matchedUser = findRandomMatchingUser(userId, userInterests);

            if (matchedUser != null) {
                ChatRoom chatRoom = createRandomChatRoom(user, matchedUser);
                return createSuccessResponse(chatRoom, matchedUser, userId);
            } else {
                return createFailureResponse("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.");
            }
        } catch (Exception e) {
            log.error("랜덤 채팅 매칭 중 오류 발생 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
            return createFailureResponse("매칭 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));
    }

    private List<String> getUserInterests(User user, RandomChatStartRequestDto requestDto) {
        List<String> interests = requestDto.getInterests();
        if (interests == null || interests.isEmpty()) {
            interests = user.getInterests();
        }
        return interests != null ? interests : new ArrayList<>();
    }

    private User findRandomMatchingUser(Long userId, List<String> interests) {
        Set<Long> blockedUserIds = getBlockedUserIds(userId);
        Set<User> candidateUsers = new HashSet<>();

        for (String interest : interests) {
            try {
                List<User> usersWithInterest = userRepository.findActiveUsersByInterest(interest);
                candidateUsers.addAll(usersWithInterest);
            } catch (Exception e) {
                log.warn("관심사 '{}' 사용자 조회 실패: {}", interest, e.getMessage());
            }
        }

        List<User> eligibleUsers = candidateUsers.stream()
                .filter(candidate -> isEligibleForMatching(candidate, userId, blockedUserIds))
                .toList();

        return selectRandomUser(eligibleUsers);
    }

    private boolean isEligibleForMatching(User candidate, Long userId, Set<Long> blockedUserIds) {
        if (candidate.getId().equals(userId)) {
            return false;
        }

        if (blockedUserIds.contains(candidate.getId())) {
            return false;
        }

        if (!candidate.isRandomChatEnabled()) {
            return false;
        }

        if (isBlockedByUser(userId, candidate.getId())) {
            return false;
        }

        return true;
    }

    private User selectRandomUser(List<User> eligibleUsers) {
        if (eligibleUsers.isEmpty()) {
            return null;
        }

        Random random = new Random();
        return eligibleUsers.get(random.nextInt(eligibleUsers.size()));
    }

    private Set<Long> getBlockedUserIds(Long userId) {
        try {
            return friendBlockRepository.findBlockedUsers(userId).stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("차단 사용자 목록 조회 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
            return new HashSet<>();
        }
    }

    private boolean isBlockedByUser(Long userId, Long targetUserId) {
        try {
            User user = findUserById(userId);
            User targetUser = findUserById(targetUserId);
            return friendBlockRepository.findByBlockerAndBlocked(targetUser, user).isPresent();
        } catch (Exception e) {
            log.warn("차단 여부 확인 실패 - 사용자: {} -> {}, 오류: {}", targetUserId, userId, e.getMessage());
            return false;
        }
    }

    private ChatRoom createRandomChatRoom(User user1, User user2) {
        String chatRoomName = "랜덤 채팅 - " + user1.getNickname() + " & " + user2.getNickname();

        ChatRoom chatRoom = ChatRoom.builder()
                .name(chatRoomName)
                .type(ChatRoomType.RANDOM)
                .owner(user1)
                .isActive(true)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        addUserToChatRoom(savedChatRoom, user1);
        addUserToChatRoom(savedChatRoom, user2);

        log.info("랜덤 채팅방 생성 완료 - ID: {}, 참여자: {} & {}",
                savedChatRoom.getId(), user1.getId(), user2.getId());

        return savedChatRoom;
    }

    private void addUserToChatRoom(ChatRoom chatRoom, User user) {
        ChatRoomUser roomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .notificationEnabled(true)
                .build();
        chatRoomUserRepository.save(roomUser);
    }

    private RandomChatMatchResponseDto createSuccessResponse(ChatRoom chatRoom, User matchedUser, Long userId) {
        return RandomChatMatchResponseDto.builder()
                .matched(true)
                .waiting(false)
                .chatRoom(buildChatRoomResponse(chatRoom, userId))
                .matchedUser(buildChatUserResponse(matchedUser))
                .matchedAt(LocalDateTime.now())
                .message("매칭이 완료되었습니다!")
                .build();
    }

    private RandomChatMatchResponseDto createFailureResponse(String message) {
        return RandomChatMatchResponseDto.builder()
                .matched(false)
                .waiting(false)
                .message(message)
                .build();
    }

    private ChatRoomResponseDto buildChatRoomResponse(ChatRoom chatRoom, Long currentUserId) {
        ChatRoomResponseDto dto = ChatRoomResponseDto.from(chatRoom, currentUserId);

        List<ChatRoomUser> roomUsers = chatRoomUserRepository.findAllByChatRoomId(chatRoom.getId());
        List<ChatUserResponseDto> users = roomUsers.stream()
                .map(roomUser -> buildChatUserResponse(roomUser.getUser()))
                .toList();

        dto.setUsers(users);
        return dto;
    }

    private ChatUserResponseDto buildChatUserResponse(User user) {
        String thumbnailUrl = null;
        try {
            thumbnailUrl = userProfileImageService.getProfileImage(user.getId()).getThumbnailImageUrl();
        } catch (Exception e) {
            log.warn("프로필 이미지 조회 실패 - 사용자: {}, 오류: {}", user.getId(), e.getMessage());
        }
        return ChatUserResponseDto.from(user, thumbnailUrl);
    }
}