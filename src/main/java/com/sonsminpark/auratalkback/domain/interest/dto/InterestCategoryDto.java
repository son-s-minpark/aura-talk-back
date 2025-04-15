package com.sonsminpark.auratalkback.domain.interest.dto;

import com.sonsminpark.auratalkback.domain.interest.dto.response.InterestResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestCategoryDto {
    private String category;
    private List<InterestResponseDto> interests;
}