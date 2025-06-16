package com.sonsminpark.auratalkback.domain.friend.entity;

import lombok.Getter;

@Getter
public enum FriendStatus {
    REQUEST_SENT("내가 친구 요청을 보낸 상태"),
    REQUEST_RECEIVED("상대방이 나에게 친구 요청을 보낸 상태"),
    NONE("아무 관계가 없는 상태"),
    BLOCKED("내가 상대방을 차단한 상태"),
    BLOCKED_BY("상대방이 나를 차단한 상태"),
    FRIENDS("이미 친구인 상태");

    private final String description;

    FriendStatus(String description) {
        this.description = description;
    }
}
