package com.sonsminpark.auratalkback.domain.friend.dto.response;

import com.sonsminpark.auratalkback.domain.friend.entity.FriendStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendRequestResponseDto {
    private FriendStatus status;
    private Long requesterId;
    private Long recipientId;

}
