package com.sonsminpark.auratalkback.performance;

import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class N1PerformanceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long testUserId;
    private List<Long> chatRoomIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User testUser = createTestUser("test@example.com", "TestUser");
        testUserId = testUser.getId();

        // 10개의 채팅방 생성, 각 채팅방에 5명의 사용자
        for (int i = 0; i < 10; i++) {
            ChatRoom chatRoom = createChatRoomWithUsers("TestRoom" + i, 5);
            chatRoomIds.add(chatRoom.getId());
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("채팅방 목록 조회 - N+1 문제 발생 시나리오")
    void testChatRoomListN1Problem() {
        log.info("=== N+1 Problem Test Start ===");

        long startTime = System.currentTimeMillis();

        List<ChatRoomResponseDto> chatRooms = chatService.getChatRoomsByUserId(testUserId);

        long endTime = System.currentTimeMillis();

        log.info("Total Execution Time: {} ms", endTime - startTime);
        log.info("Retrieved {} chat rooms", chatRooms.size());

        for (ChatRoomResponseDto chatRoom : chatRooms) {
            log.info("ChatRoom {}: {} users", chatRoom.getId(), chatRoom.getUsers().size());
        }

        log.info("=== N+1 Problem Test End ===");
    }

    @Test
    @DisplayName("채팅방 상세 조회 - 성능 측정")
    void testChatRoomDetailPerformance() {
        log.info("=== ChatRoom Detail Performance Test Start ===");

        Long chatRoomId = chatRoomIds.get(0);

        long startTime = System.currentTimeMillis();

        ChatRoomResponseDto chatRoom = chatService.getChatRoomInfo(chatRoomId, testUserId);

        long endTime = System.currentTimeMillis();

        log.info("Execution Time: {} ms", endTime - startTime);
        log.info("ChatRoom: {}, Users: {}", chatRoom.getName(), chatRoom.getUsers().size());

        log.info("=== ChatRoom Detail Performance Test End ===");
    }

    @Test
    @DisplayName("메시지 목록 조회 - 성능 측정")
    void testMessageListPerformance() {
        log.info("=== Message List Performance Test Start ===");

        // 첫 번째 채팅방에 메시지 100개 생성
        Long chatRoomId = chatRoomIds.get(0);
        createTestMessages(chatRoomId, 100);

        entityManager.flush();
        entityManager.clear();

        long startTime = System.currentTimeMillis();

        var messages = chatService.getMessages(chatRoomId, testUserId,
                org.springframework.data.domain.PageRequest.of(0, 50));

        long endTime = System.currentTimeMillis();

        log.info("Execution Time: {} ms", endTime - startTime);
        log.info("Retrieved {} messages", messages.getContent().size());

        log.info("=== Message List Performance Test End ===");
    }

    private User createTestUser(String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password("password")
                .username(nickname)
                .nickname(nickname)
                .build();
        return userRepository.save(user);
    }

    private ChatRoom createChatRoomWithUsers(String roomName, int userCount) {
        User owner = createTestUser(roomName + "_owner@example.com", roomName + "_Owner");

        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomName)
                .type(ChatRoomType.GROUP)
                .owner(owner)
                .isActive(true)
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);

        // 사용자 추가
        for (int i = 0; i < userCount; i++) {
            User user = createTestUser(roomName + "_user" + i + "@example.com",
                    roomName + "_User" + i);
        }

        return chatRoom;
    }

    private void createTestMessages(Long chatRoomId, int count) {
    }
}