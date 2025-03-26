package com.sonsminpark.auratalkback.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileSetupRequestDto {
    @NotBlank(message = "사용자명은 필수 입력값입니다.")
    @Size(min = 3, max = 15, message = "사용자명은 3자 이상 15자 이하이어야 합니다.")
    @Pattern(regexp = "^[^\\s]+$", message = "사용자명에 공백을 포함할 수 없습니다.")
    private String username;

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 1, max = 15, message = "닉네임은 1자 이상 15자 이하이어야 합니다.")
    @Pattern(regexp = "^[^\\s]+$", message = "닉네임에 공백을 포함할 수 없습니다.")
    private String nickname;

    private List<String> interests;
}