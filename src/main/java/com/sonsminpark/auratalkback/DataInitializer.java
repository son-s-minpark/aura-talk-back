package com.sonsminpark.auratalkback;

import com.sonsminpark.auratalkback.domain.chat.entity.*;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatMessageRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.domain.user.service.UserProfileImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileImageService userProfileImageService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== 테스트 데이터 생성 시작 ===");

        // 1. 사용자 생성
        List<User> users = createUsers(20);

        // 2. 채팅방 생성
        List<ChatRoom> chatRooms = createChatRooms(users, 10);

        // 3. 메시지 생성
        createMessages(chatRooms, users);

        log.info("=== 테스트 데이터 생성 완료 ===");
    }

    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            User user = User.builder()
                    .email("user" + i + "@example.com")
                    .password(passwordEncoder.encode("Test123!"))
                    .username("user" + i)
                    .nickname("사용자" + i)
                    .description("안녕하세요, 사용자" + i + "입니다.")
                    .emailVerified(true)
                    .build();

            user = userRepository.save(user);

            try {
                userProfileImageService.createDefaultProfileImage(user.getId());
            } catch (Exception e) {
                log.warn("프로필 이미지 생성 실패 - 사용자 {}: {}", user.getId(), e.getMessage());
            }

            user = userRepository.findByIdWithProfileImage(user.getId())
                    .orElse(user);

            users.add(user);
        }

        log.info("{}명의 사용자 생성 완료", count);
        return users;
    }

    private List<ChatRoom> createChatRooms(List<User> users, int count) {
        List<ChatRoom> chatRooms = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            User owner = users.get(i % users.size());

            ChatRoom chatRoom = ChatRoom.builder()
                    .name("채팅방 " + i)
                    .type(ChatRoomType.GROUP)
                    .owner(owner)
                    .isActive(true)
                    .build();

            chatRoom = chatRoomRepository.save(chatRoom);

            // 채팅방에 5~10명의 사용자 추가
            int userCount = 5 + (i % 6);
            for (int j = 0; j < userCount; j++) {
                User user = users.get((i + j) % users.size());

                ChatRoomUser roomUser = ChatRoomUser.builder()
                        .chatRoom(chatRoom)
                        .user(user)
                        .notificationEnabled(true)
                        .build();

                chatRoomUserRepository.save(roomUser);
            }

            chatRooms.add(chatRoom);
        }

        log.info("{}개의 채팅방 생성 완료", count);
        return chatRooms;
    }

    private void createMessages(List<ChatRoom> chatRooms, List<User> users) {
        int totalMessages = 0;

        for (ChatRoom chatRoom : chatRooms) {
            // 각 채팅방에 20~50개의 메시지 생성
            int messageCount = 20 + (int) (Math.random() * 31);

            for (int i = 0; i < messageCount; i++) {
                User sender = users.get((int) (Math.random() * users.size()));

                ChatMessage message = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .sender(sender)
                        .content("안녕하세요! 메시지 " + (i + 1) + "번입니다.")
                        .type(MessageType.TEXT)
                        .build();

                chatMessageRepository.save(message);
                totalMessages++;
            }

            chatRoom.updateLastMessageAt();
        }

        log.info("총 {}개의 메시지 생성 완료", totalMessages);
    }
}