package com.sonsminpark.auratalkback.domain.friend.repository;

import com.sonsminpark.auratalkback.domain.friend.entity.FriendBlock;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendBlockRepository extends JpaRepository<FriendBlock, Long> {

    Optional<FriendBlock> findByBlockerAndBlocked(User blocker, User blocked);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "JOIN FriendBlock b ON b.blocked = u " +
            "WHERE b.blocker.id = :userId")
    List<User> findBlockedUsers(@Param("userId") Long userId);
}
