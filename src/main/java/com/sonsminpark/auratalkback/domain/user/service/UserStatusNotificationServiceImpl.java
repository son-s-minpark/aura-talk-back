package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.friend.service.FriendService;
import com.sonsminpark.auratalkback.domain.user.dto.response.UserStatusNotificationDto;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.global.websocket.WebSocketTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserStatusNotificationServiceImpl implements UserStatusNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final FriendService friendService;

    @Override
    public void notifyFriendsStatusChange(User user) {
        List<Long> friendIds = friendService.getFriendIds(user.getId());

        if (friendIds.isEmpty()) {
            return;
        }

        for (Long friendId : friendIds) {
            try {
                sendStatusChangeNotification(friendId, user);
            } catch (Exception e) {
                log.error("WebSocket 상태 알림 전송 실패 - 사용자: {}, 친구: {}, 오류: {}",
                        user.getId(), friendId, e.getMessage());
            }
        }

        log.info("WebSocket 상태 알림 전송 완료 - 사용자: {}, 상태: {}, 친구 수: {}명",
                user.getId(), user.getStatus(), friendIds.size());
    }

    @Override
    public void sendStatusChangeNotification(Long userId, User statusChangeUser) {
        UserStatusNotificationDto notification = UserStatusNotificationDto.from(statusChangeUser);
        String topic = WebSocketTopics.getUserFriendStatusTopic(userId);

        messagingTemplate.convertAndSend(topic, notification);
    }


}
