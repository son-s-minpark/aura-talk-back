package com.sonsminpark.auratalkback.domain.interest.service;

import com.sonsminpark.auratalkback.domain.interest.dto.InterestCategoryDto;
import com.sonsminpark.auratalkback.domain.interest.dto.response.InterestResponseDto;
import com.sonsminpark.auratalkback.domain.interest.dto.response.InterestUsersResponseDto;
import com.sonsminpark.auratalkback.domain.interest.entity.Interest;
import com.sonsminpark.auratalkback.domain.interest.repository.InterestRepository;
import com.sonsminpark.auratalkback.domain.user.dto.response.UserResponseDto;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestService {

    private final InterestRepository interestRepository;
    private final UserRepository userRepository;

    // 애플리케이션 시작 시 기본 관심사 초기화
    @PostConstruct
    @Transactional
    public void initInterests() {
        if (interestRepository.count() == 0) {
            log.info("기본 관심사 데이터 초기화 중...");
            List<Interest> defaultInterests = Interest.createDefaultInterests();
            interestRepository.saveAll(defaultInterests);
            log.info("기본 관심사 데이터 {} 개 초기화 완료", defaultInterests.size());
        }
    }

    @Transactional(readOnly = true)
    public List<InterestCategoryDto> getAllInterestsByCategory() {
        List<String> categories = interestRepository.findAllCategories();

        return categories.stream()
                .map(category -> {
                    List<Interest> interests = interestRepository.findByCategory(category);
                    return InterestCategoryDto.builder()
                            .category(category)
                            .interests(InterestResponseDto.fromList(interests))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InterestUsersResponseDto getUsersByInterestName(String interestName) {
        Interest interest = interestRepository.findByName(interestName)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 관심사입니다: " + interestName));

        List<User> users = userRepository.findAll().stream()
                .filter(user -> !user.isDeleted() && user.getInterests().contains(interestName))
                .collect(Collectors.toList());

        List<UserResponseDto> userDtos = users.stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());

        return InterestUsersResponseDto.builder()
                .interestName(interestName)
                .users(userDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InterestResponseDto> getInterestsByCategory(String category) {
        List<Interest> interests = interestRepository.findByCategory(category);
        return InterestResponseDto.fromList(interests);
    }
}