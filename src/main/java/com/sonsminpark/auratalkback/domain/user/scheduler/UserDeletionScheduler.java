package com.sonsminpark.auratalkback.domain.user.scheduler;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionScheduler {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    @Transactional
    public void processExpiredUsers() {
        log.info("탈퇴 사용자 정보 익명화 작업 시작...");

        // Redis에서 삭제 예정인 사용자 ID 목록 조회
        Set<String> keys = redisTemplate.keys("USER_DELETION:*");
        if (keys == null || keys.isEmpty()) {
            log.info("처리할 만료된 탈퇴 사용자가 없습니다.");
            return;
        }

        // 키에서 사용자 ID 추출
        List<Long> userIds = keys.stream()
                .map(key -> key.replace("USER_DELETION:", ""))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        log.info("익명화 처리할 사용자 수: {}", userIds.size());

        // 해당 사용자들 조회 및 익명화 처리
        List<User> usersToAnonymize = userRepository.findAllById(userIds);
        for (User user : usersToAnonymize) {
            if (user.isDeleted()) {
                log.info("사용자 {} 익명화 처리 중...", user.getId());
                // 개인정보 익명화
                user.anonymize();
                // Redis에서 삭제 키 제거
                redisTemplate.delete("USER_DELETION:" + user.getId());
            }
        }

        log.info("탈퇴 사용자 정보 익명화 작업 완료");
    }

    // 탈퇴 후 30일 이상 경과한 사용자 익명화
    @Scheduled(cron = "0 0 1 * * ?") // 매일 새벽 1시에 실행
    @Transactional
    public void anonymizeExpiredDeletedUsers() {
        log.info("기간이 경과한 탈퇴 사용자 확인 및 익명화 작업 시작...");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<User> expiredUsers = userRepository.findByIsDeletedTrueAndDeletedAtBefore(thirtyDaysAgo);

        log.info("30일 경과 익명화 대상 사용자 수: {}", expiredUsers.size());

        for (User user : expiredUsers) {
            log.info("사용자 {} 익명화 처리 중...", user.getId());
            user.anonymize();
        }

        log.info("기간 경과 탈퇴 사용자 익명화 작업 완료");
    }
}