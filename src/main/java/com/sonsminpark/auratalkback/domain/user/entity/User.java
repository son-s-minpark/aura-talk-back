package com.sonsminpark.auratalkback.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
@NamedEntityGraph(
        name = "User.withProfileAndInterests",
        attributeNodes = {
                @NamedAttributeNode("userProfileImage"),
                @NamedAttributeNode("userInterests")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 15)
    private String username;

    @Column(nullable = false, length = 15)
    private String nickname;

    @Column(length = 100)
    private String description;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @BatchSize(size = 10)
    private List<UserInterest> userInterests = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ONLINE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean randomChatEnabled = false;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfileImage userProfileImage;

    public void updateStatus(UserStatus status) {
        if (this.status != UserStatus.SECRET) {
            this.status = status;
        }
    }

    public void setSecretStatus(boolean isSecret) {
        if (isSecret) {
            this.status = UserStatus.SECRET;
        }
        else {
            this.status = UserStatus.ONLINE;
        }
    }
    public List<String> getInterests() {
        return userInterests.stream()
                .map(UserInterest::getInterestName)
                .toList();
    }

    public void update(String nickname, List<String> interests) {
        this.nickname = nickname;
        updateInterests(interests);
    }

    public void updateProfile(String username, String nickname, List<String> interests, String description) {
        this.username = username;
        this.nickname = nickname;
        this.description = description;
        updateInterests(interests);
    }

    private void updateInterests(List<String> interests) {
        this.userInterests.clear();
        if (interests != null) {
            for (String interest : interests) {
                this.userInterests.add(new UserInterest(this, interest));
            }
        }
    }


    public void updateChatSettings(boolean randomChatEnabled) {
        this.randomChatEnabled = randomChatEnabled;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.status = UserStatus.OFFLINE;
    }

    // 탈퇴한 사용자 정보 익명화
    public void anonymize() {
        this.email = "deleted_" + this.id + "_" + System.currentTimeMillis() + "@deleted.com";
        this.username = "탈퇴회원";
        this.nickname = "탈퇴회원";
        this.password = "";
        this.userInterests.clear();
    }
}