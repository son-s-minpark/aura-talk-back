package com.sonsminpark.auratalkback.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class ChatRoomCreateRequestDto {

    @NotBlank(message = "채팅방 이름은 필수 입력값입니다.")
    @Size(min = 1, max = 50, message = "채팅방 이름은 1자 이상 50자 이하이어야 합니다.")
    private String name;

    private List<Long> userIds;

    private String roomImageS3Key;
}