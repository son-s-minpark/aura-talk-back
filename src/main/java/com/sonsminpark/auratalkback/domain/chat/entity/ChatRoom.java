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
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChatRoomType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastMessageAt;

    @Column(unique = true)
    private String inviteCode;

    @Column
    private LocalDateTime inviteCodeExpiredAt;

    @Column
    private String roomImageUrl;

    public void updateName(String name) {
        this.name = name;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void generateInviteCode(String code, LocalDateTime expiredAt) {
        this.inviteCode = code;
        this.inviteCodeExpiredAt = expiredAt;
    }

    public void updateLastMessageAt() {
        this.lastMessageAt = LocalDateTime.now();
    }

    public boolean isInviteCodeValid() {
        return inviteCode != null &&
                inviteCodeExpiredAt != null &&
                inviteCodeExpiredAt.isAfter(LocalDateTime.now());
    }

    public void updateRoomImage(String imageUrl) {
        this.roomImageUrl = imageUrl;
    }

    public boolean isUserOwner(Long userId) {
        return this.owner != null && this.owner.getId().equals(userId);
    }

    public void updateOwner(User newOwner) {
        this.owner = newOwner;
    }
}