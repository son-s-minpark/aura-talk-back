package com.sonsminpark.auratalkback.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeleteRequestDto {
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    private String password;
}