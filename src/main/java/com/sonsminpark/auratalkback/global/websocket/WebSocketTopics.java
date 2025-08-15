package com.sonsminpark.auratalkback.global.websocket;

public final class WebSocketTopics {

    private WebSocketTopics() {
    }

    public static final String USER_FRIENDS_STATUS = "/topic/user/";
    public static final String USER_FRIENDS_STATUS_SUFFIX = "/friends-status";

    public static String getUserFriendStatusTopic(Long userId) {
        return USER_FRIENDS_STATUS + userId + USER_FRIENDS_STATUS_SUFFIX;
    }
}
