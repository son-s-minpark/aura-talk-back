package com.sonsminpark.auratalkback.domain.interest.dto.response;

import com.sonsminpark.auratalkback.domain.interest.entity.Interest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestResponseDto {
    private Long id;
    private String name;
    private String category;

    public static InterestResponseDto from(Interest interest) {
        return InterestResponseDto.builder()
                .id(interest.getId())
                .name(interest.getName())
                .category(interest.getCategory())
                .build();
    }

    public static List<InterestResponseDto> fromList(List<Interest> interests) {
        return interests.stream()
                .map(InterestResponseDto::from)
                .collect(Collectors.toList());
    }
}