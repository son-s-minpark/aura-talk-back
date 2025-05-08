package com.sonsminpark.auratalkback.domain.user.repository;

import com.sonsminpark.auratalkback.domain.user.entity.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {

    Optional<UserProfileImage> findByUserId(Long userId);
}
