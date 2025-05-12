package com.sonsminpark.auratalkback.domain.chat.dto.request;

import com.sonsminpark.auratalkback.domain.chat.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequestDto {

    @NotBlank(message = "메시지 내용은 필수 입력값입니다.")
    private String content;

    @NotNull(message = "메시지 타입은 필수 입력값입니다.")
    private MessageType type;
}