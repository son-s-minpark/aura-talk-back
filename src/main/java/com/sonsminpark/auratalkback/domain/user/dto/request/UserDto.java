package com.sonsminpark.auratalkback.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class UserDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignUpRequest {
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "비밀번호는 대소문자, 특수문자, 숫자를 포함해야 합니다.")
        private String password;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        @Size(min = 1, max = 15, message = "닉네임은 1자 이상 15자 이하이어야 합니다.")
        @Pattern(regexp = "^[^\\s]+$", message = "닉네임에 공백을 포함할 수 없습니다.")
        private String nickname;

        private List<String> interests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String tokenType;
        private Long expiresIn;
    }
}