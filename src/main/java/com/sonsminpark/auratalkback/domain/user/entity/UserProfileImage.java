package com.sonsminpark.auratalkback.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_profile_images")
public class UserProfileImage {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 1000)
    private String imageUrl;


    public void updateProfileImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}