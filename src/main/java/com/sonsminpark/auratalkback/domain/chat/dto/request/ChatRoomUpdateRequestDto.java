package com.sonsminpark.auratalkback.domain.chat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomUpdateRequestDto {

    @Size(min = 1, max = 50, message = "채팅방 이름은 1자 이상 50자 이하이어야 합니다.")
    private String name;

    private String roomImageUrl;
}