package com.sonsminpark.auratalkback.performance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Slf4j
@Component
@Profile("performance")
@RequiredArgsConstructor
public class PerformanceRunner implements CommandLineRunner {

    private final RestTemplate restTemplate = new RestTemplate();
    private String authToken;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Starting Performance Test ===");

        // 1. 로그인
        login();

        // 2. 채팅방 목록 조회 테스트
        testChatRoomList();

        // 3. 메시지 목록 조회 테스트
        testMessageList();

        log.info("=== Performance Test Complete ===");
    }

    private void login() {
        String url = "http://localhost:8080/api/users/login";
        String requestBody = """
            {
                "email": "test@example.com",
                "password": "Test123!"
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        this.authToken = "Bearer " + extractToken(response.getBody());
    }

    private void testChatRoomList() {
        log.info("\n=== Testing ChatRoom List API ===");

        String url = "http://localhost:8080/api/chatrooms";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

        long endTime = System.currentTimeMillis();

        log.info("API Response Time: {} ms", endTime - startTime);
        log.info("Response Status: {}", response.getStatusCode());
    }

    private void testMessageList() {
        log.info("\n=== Testing Message List API ===");

        String url = "http://localhost:8080/api/chats/1?page=0&size=50";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

        long endTime = System.currentTimeMillis();

        log.info("API Response Time: {} ms", endTime - startTime);
        log.info("Response Status: {}", response.getStatusCode());
    }

    private String extractToken(String responseBody) {
        return "dummy-token";
    }
}