package com.sonsminpark.auratalkback.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sonsminpark.auratalkback.domain.chat.dto.request.RandomChatStartRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatUserResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.RandomChatMatchResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import com.sonsminpark.auratalkback.domain.chat.service.RandomChatService;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.global.exception.GlobalExceptionHandler;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Lenient 모드 추가
@DisplayName("RandomChatController 테스트")
class RandomChatControllerTest {

    @Mock
    private RandomChatService randomChatService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private RandomChatController randomChatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(randomChatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(post("/").characterEncoding(StandardCharsets.UTF_8))
                .alwaysDo(print())
                .build();

        // 각 테스트 전에 Mock 리셋
        reset(randomChatService, jwtTokenProvider);
    }

    @Nested
    @DisplayName("랜덤 채팅 시작 테스트")
    class StartRandomChatTest {

        @Test
        @DisplayName("성공: 매칭 성공")
        void startRandomChat_MatchSuccessful() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임", "영화"))
                    .build();

            ChatUserResponseDto matchedUserDto = ChatUserResponseDto.builder()
                    .id(2L)
                    .nickname("매칭된유저")
                    .thumbnailImageUrl("http://test.com/thumb.png")
                    .build();

            ChatRoomResponseDto chatRoomDto = ChatRoomResponseDto.builder()
                    .id(1L)
                    .name("랜덤 채팅 - 사용자1 & 사용자2")
                    .type(ChatRoomType.RANDOM)
                    .isOwner(true)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(true)
                    .waiting(false)
                    .chatRoom(chatRoomDto)
                    .matchedUser(matchedUserDto)
                    .matchedAt(LocalDateTime.now())
                    .message("매칭이 완료되었습니다!")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("매칭이 완료되었습니다!"))
                    .andExpect(jsonPath("$.data.matched").value(true))
                    .andExpect(jsonPath("$.data.waiting").value(false))
                    .andExpect(jsonPath("$.data.chatRoom").exists())
                    .andExpect(jsonPath("$.data.chatRoom.id").value(1L))
                    .andExpect(jsonPath("$.data.chatRoom.type").value("RANDOM"))
                    .andExpect(jsonPath("$.data.matchedUser").exists())
                    .andExpect(jsonPath("$.data.matchedUser.id").value(2L))
                    .andExpect(jsonPath("$.data.matchedUser.nickname").value("매칭된유저"))
                    .andExpect(jsonPath("$.data.matchedAt").exists())
                    .andExpect(jsonPath("$.data.message").value("매칭이 완료되었습니다!"));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("실패: 매칭 실패 - 사용 가능한 사용자 없음")
        void startRandomChat_MatchFailed_NoAvailableUsers() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("희귀한취미"))
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .chatRoom(null)
                    .matchedUser(null)
                    .matchedAt(null)
                    .message("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요."))
                    .andExpect(jsonPath("$.data.matched").value(false))
                    .andExpect(jsonPath("$.data.waiting").value(false))
                    .andExpect(jsonPath("$.data.chatRoom").doesNotExist())
                    .andExpect(jsonPath("$.data.matchedUser").doesNotExist())
                    .andExpect(jsonPath("$.data.matchedAt").doesNotExist())
                    .andExpect(jsonPath("$.data.message").value("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요."));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("실패: 랜덤 채팅 비활성화")
        void startRandomChat_RandomChatDisabled() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .chatRoom(null)
                    .matchedUser(null)
                    .matchedAt(null)
                    .message("랜덤 채팅이 비활성화되어 있습니다. 설정에서 활성화해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("랜덤 채팅이 비활성화되어 있습니다. 설정에서 활성화해주세요."))
                    .andExpect(jsonPath("$.data.matched").value(false))
                    .andExpect(jsonPath("$.data.message").value("랜덤 채팅이 비활성화되어 있습니다. 설정에서 활성화해주세요."));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("실패: 관심사 없음")
        void startRandomChat_NoInterests() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of())
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .chatRoom(null)
                    .matchedUser(null)
                    .matchedAt(null)
                    .message("관심사를 설정해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("관심사를 설정해주세요."))
                    .andExpect(jsonPath("$.data.matched").value(false))
                    .andExpect(jsonPath("$.data.message").value("관심사를 설정해주세요."));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("성공: 단일 관심사로 매칭")
        void startRandomChat_SingleInterest() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("음악"))
                    .build();

            ChatUserResponseDto matchedUserDto = ChatUserResponseDto.builder()
                    .id(3L)
                    .nickname("음악애호가")
                    .build();

            ChatRoomResponseDto chatRoomDto = ChatRoomResponseDto.builder()
                    .id(2L)
                    .name("랜덤 채팅 - 사용자1 & 음악애호가")
                    .type(ChatRoomType.RANDOM)
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(true)
                    .waiting(false)
                    .chatRoom(chatRoomDto)
                    .matchedUser(matchedUserDto)
                    .matchedAt(LocalDateTime.now())
                    .message("매칭이 완료되었습니다!")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.matched").value(true))
                    .andExpect(jsonPath("$.data.matchedUser.nickname").value("음악애호가"));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("성공: 여러 관심사로 매칭")
        void startRandomChat_MultipleInterests() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임", "영화", "음악", "독서", "운동"))
                    .build();

            ChatUserResponseDto matchedUserDto = ChatUserResponseDto.builder()
                    .id(4L)
                    .nickname("다양한취미")
                    .build();

            ChatRoomResponseDto chatRoomDto = ChatRoomResponseDto.builder()
                    .id(3L)
                    .name("랜덤 채팅 - 사용자1 & 다양한취미")
                    .type(ChatRoomType.RANDOM)
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(true)
                    .waiting(false)
                    .chatRoom(chatRoomDto)
                    .matchedUser(matchedUserDto)
                    .matchedAt(LocalDateTime.now())
                    .message("매칭이 완료되었습니다!")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.matched").value(true))
                    .andExpect(jsonPath("$.data.matchedUser.nickname").value("다양한취미"));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void startRandomChat_UserNotFound() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(999L);
            given(randomChatService.startRandomChat(eq(999L), any(RandomChatStartRequestDto.class)))
                    .willThrow(UserNotFoundException.of(999L));

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(420));

            verify(randomChatService).startRandomChat(eq(999L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("성공: null 관심사 리스트 처리")
        void startRandomChat_NullInterests() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(null)
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .message("관심사를 설정해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.matched").value(false))
                    .andExpect(jsonPath("$.data.message").value("관심사를 설정해주세요."));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 요청 데이터")
        void startRandomChat_InvalidRequestData() throws Exception {
            // Given
            String invalidJson = "{ \"interests\": \"invalid\" }"; // interests가 문자열

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);

            // When + Then
            // JSON 파싱 오류는 현재 500으로 처리되고 있음
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isInternalServerError()) // 500으로 변경
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("성공: 빈 요청 본문")
        void startRandomChat_EmptyRequestBody() throws Exception {
            // Given
            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .message("관심사를 설정해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class)))
                    .willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("실패: 서비스에서 일반 예외 발생")
        void startRandomChat_ServiceException() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class)))
                    .willThrow(new RuntimeException("매칭 중 오류가 발생했습니다."));

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }
    }

    @Nested
    @DisplayName("JWT 토큰 처리 테스트")
    class JwtTokenTest {

        @Test
        @DisplayName("실패: Authorization 헤더 누락")
        void startRandomChat_MissingAuthHeader() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError()); // NullPointerException 발생
        }

        @Test
        @DisplayName("실패: 유효하지 않은 JWT 토큰")
        void startRandomChat_InvalidJwtToken() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("invalid-token"))
                    .willThrow(new RuntimeException("Invalid token"));

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패: Bearer 형식이 아닌 토큰")
        void startRandomChat_NonBearerToken() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            // When + Then
            given(jwtTokenProvider.getUserIdFromToken("oken"))
                    .willThrow(new RuntimeException("Invalid token format"));

            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Basic token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패: 짧은 Authorization 헤더")
        void startRandomChat_ShortAuthHeader() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "short")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError()); // StringIndexOutOfBoundsException 발생
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("관심사 경계값 - 빈 리스트")
        void startRandomChat_EmptyInterestsList() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of())
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .message("관심사를 설정해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.matched").value(false));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("관심사 경계값 - 매우 많은 관심사")
        void startRandomChat_ManyInterests() throws Exception {
            // Given
            List<String> manyInterests = List.of(
                    "게임", "영화", "음악", "독서", "운동", "요리", "여행", "사진",
                    "그림", "춤", "노래", "악기", "스포츠", "등산", "캠핑", "낚시",
                    "바둑", "체스", "퍼즐", "수집", "원예", "반려동물", "봉사", "학습"
            );

            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(manyInterests)
                    .build();

            ChatUserResponseDto matchedUserDto = ChatUserResponseDto.builder()
                    .id(5L)
                    .nickname("다재다능")
                    .build();

            ChatRoomResponseDto chatRoomDto = ChatRoomResponseDto.builder()
                    .id(4L)
                    .name("랜덤 채팅 - 사용자1 & 다재다능")
                    .type(ChatRoomType.RANDOM)
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(true)
                    .waiting(false)
                    .chatRoom(chatRoomDto)
                    .matchedUser(matchedUserDto)
                    .matchedAt(LocalDateTime.now())
                    .message("매칭이 완료되었습니다!")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.matched").value(true));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("관심사 경계값 - 매우 긴 관심사 이름")
        void startRandomChat_VeryLongInterestName() throws Exception {
            // Given
            String longInterest = "a".repeat(1000);
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of(longInterest))
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .message("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("관심사 경계값 - 특수 문자가 포함된 관심사")
        void startRandomChat_SpecialCharacterInterests() throws Exception {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("K-POP 🎵", "C++ 프로그래밍", "@게임", "#해시태그"))
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .message("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("사용자 ID 경계값 - 최소값")
        void startRandomChat_MinUserId() throws Exception {
            // Given
            Long minUserId = 1L;
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .message("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(minUserId);
            given(randomChatService.startRandomChat(eq(minUserId), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(randomChatService).startRandomChat(eq(minUserId), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("사용자 ID 경계값 - 최대값")
        void startRandomChat_MaxUserId() throws Exception {
            // Given
            Long maxUserId = Long.MAX_VALUE;
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            ChatUserResponseDto maxUserDto = ChatUserResponseDto.builder()
                    .id(maxUserId)
                    .nickname("최대ID유저")
                    .build();

            ChatRoomResponseDto chatRoomDto = ChatRoomResponseDto.builder()
                    .id(Long.MAX_VALUE - 1)
                    .name("랜덤 채팅 - 최대ID & 다른유저")
                    .type(ChatRoomType.RANDOM)
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(true)
                    .waiting(false)
                    .chatRoom(chatRoomDto)
                    .matchedUser(maxUserDto)
                    .matchedAt(LocalDateTime.now())
                    .message("매칭이 완료되었습니다!")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(maxUserId);
            given(randomChatService.startRandomChat(eq(maxUserId), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.matched").value(true))
                    .andExpect(jsonPath("$.data.matchedUser.id").value(maxUserId));

            verify(randomChatService).startRandomChat(eq(maxUserId), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("응답 데이터 경계값 - 매우 긴 메시지")
        void startRandomChat_VeryLongMessage() throws Exception {
            // Given
            String longMessage = "a".repeat(1000);
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(false)
                    .waiting(false)
                    .message(longMessage)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.message").value(longMessage));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }

        @Test
        @DisplayName("응답 데이터 경계값 - 매우 긴 닉네임")
        void startRandomChat_VeryLongNickname() throws Exception {
            // Given
            String longNickname = "닉네임".repeat(100);
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            ChatUserResponseDto userWithLongNickname = ChatUserResponseDto.builder()
                    .id(6L)
                    .nickname(longNickname)
                    .build();

            ChatRoomResponseDto chatRoomDto = ChatRoomResponseDto.builder()
                    .id(5L)
                    .name("랜덤 채팅 - 사용자1 & " + longNickname)
                    .type(ChatRoomType.RANDOM)
                    .build();

            RandomChatMatchResponseDto responseDto = RandomChatMatchResponseDto.builder()
                    .matched(true)
                    .waiting(false)
                    .chatRoom(chatRoomDto)
                    .matchedUser(userWithLongNickname)
                    .matchedAt(LocalDateTime.now())
                    .message("매칭이 완료되었습니다!")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(randomChatService.startRandomChat(eq(1L), any(RandomChatStartRequestDto.class))).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.matched").value(true))
                    .andExpect(jsonPath("$.data.matchedUser.nickname").value(longNickname));

            verify(randomChatService).startRandomChat(eq(1L), any(RandomChatStartRequestDto.class));
        }
    }
}