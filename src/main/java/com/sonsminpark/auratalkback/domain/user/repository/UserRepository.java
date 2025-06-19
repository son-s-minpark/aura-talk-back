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

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Optional<User> findByIdAndIsDeletedFalse(Long userId);

    boolean existsByEmailAndIsDeletedFalse(String email);

    boolean existsByUsernameAndIsDeletedFalse(String username);

    boolean existsByNicknameAndIsDeletedFalse(String nickname);

    // 관심사별 사용자 조회 (N+1 해결)
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userInterests ui " +
            "WHERE ui.interestName = :interestName AND u.isDeleted = false")
    List<User> findActiveUsersByInterest(@Param("interestName") String interestName);

    List<User> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime date);
}