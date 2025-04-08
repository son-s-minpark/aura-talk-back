package com.sonsminpark.auratalkback.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSettingsRequestDto {
    @NotNull(message = "랜덤 채팅 활성화 여부는 필수 입력값입니다.")
    private boolean randomChatEnabled;
}