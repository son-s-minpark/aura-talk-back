package com.sonsminpark.auratalkback.domain.interest.dto.response;

import com.sonsminpark.auratalkback.domain.user.dto.response.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestUsersResponseDto {
    private String interestName;
    private List<UserResponseDto> users;
}