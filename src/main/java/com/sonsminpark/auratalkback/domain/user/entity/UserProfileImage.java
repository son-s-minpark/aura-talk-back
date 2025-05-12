package com.sonsminpark.auratalkback.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_profile_images")
@Entity
public class UserProfileImage {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private String originalImageUrl;

    @Column
    private String thumbnailImageUrl;

    @Column
    private boolean isDefaultProfileImage;

    public UserProfileImage(User user) {
        this.user = user;
        this.userId = user.getId();
        this.isDefaultProfileImage = true;
    }

    public void updateImage(String originalImageUrl, String thumbnailImageUrl, boolean isDefaultProfileImage) {
        this.originalImageUrl = originalImageUrl;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.isDefaultProfileImage = isDefaultProfileImage;
    }
}
