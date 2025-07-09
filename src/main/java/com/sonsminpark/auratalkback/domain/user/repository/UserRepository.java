package com.sonsminpark.auratalkback.domain.user.repository;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfileImage WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Optional<User> findByIdAndIsDeletedFalse(Long userId);

    boolean existsByEmailAndIsDeletedFalse(String email);

    boolean existsByUsernameAndIsDeletedFalse(String username);

    boolean existsByNicknameAndIsDeletedFalse(String nickname);

    List<User> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime date);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userInterests ui " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE ui.interestName = :interestName AND u.isDeleted = false")
    List<User> findActiveUsersByInterest(@Param("interestName") String interestName);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfileImage WHERE u.id = :userId")
    Optional<User> findByIdWithProfileImage(@Param("userId") Long userId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userProfileImage WHERE u.id IN :userIds")
    List<User> findAllByIdWithProfileImage(@Param("userIds") List<Long> userIds);
}