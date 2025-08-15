package com.sonsminpark.auratalkback.domain.user.dto.response;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusNotificationDto {

    private Long userId;
    private UserStatus status;
    private LocalDateTime timestamp;

    public static UserStatusNotificationDto from(User user) {
        return UserStatusNotificationDto.builder()
                .userId(user.getId())
                .status(user.getStatus())
                .timestamp(LocalDateTime.now())
                .build();
    }
}