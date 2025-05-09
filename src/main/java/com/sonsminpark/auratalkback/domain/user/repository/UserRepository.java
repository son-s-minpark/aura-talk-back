package com.sonsminpark.auratalkback.domain.user.repository;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<User> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime date);
}