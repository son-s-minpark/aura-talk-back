package com.sonsminpark.auratalkback.domain.chat.entity;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chatroom_users")
public class ChatRoomUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    private LocalDateTime lastReadAt;

    public void updateNotificationSetting(boolean enabled) {
        this.notificationEnabled = enabled;
    }

    public void updateLastReadAt() {
        this.lastReadAt = LocalDateTime.now();
    }
}