package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.entity.User;

public interface UserStatusNotificationService {
    // 사용자 상태 변경을 친구들에게 알림
    void notifyFriendsStatusChange(User user);

     // 특정 사용자에게 상태 변경 알림 전송
    void sendStatusChangeNotification(Long userId, User statusChangeUser);
}

