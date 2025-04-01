package com.sonsminpark.auratalkback.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
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

    @ElementCollection
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "interest")
    private List<String> interests = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean isDeleted;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean emailVerified = false;

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.status = UserStatus.OFFLINE;
    }

    public void update(String nickname, List<String> interests) {
        this.nickname = nickname;
        this.interests = interests;
    }

    public void updateProfile(String username, String nickname, List<String> interests) {
        this.username = username;
        this.nickname = nickname;
        this.interests = interests;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    // 탈퇴한 사용자 정보 익명화
    public void anonymize() {
        this.email = "deleted_" + this.id + "_" + System.currentTimeMillis() + "@deleted.com";
        this.username = "탈퇴회원";
        this.nickname = "탈퇴회원";
        this.password = "";
        this.interests.clear();
    }
}