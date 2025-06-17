package com.sonsminpark.auratalkback.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneToOneChatRequestDto {

    @NotNull(message = "대화할 사용자 ID는 필수 입력값입니다.")
    private Long targetUserId;
}