package com.sonsminpark.auratalkback.domain.friend.repository;

import com.sonsminpark.auratalkback.domain.friend.entity.Friend;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE u.id IN (" +
            "   SELECT f.user2.id FROM Friend f WHERE f.user1.id = :userId " +
            "   UNION " +
            "   SELECT f.user1.id FROM Friend f WHERE f.user2.id = :userId)")
    List<User> findFriendUsers(@Param("userId") Long userId);

    @Query("SELECT f FROM Friend f WHERE " +
            "(f.user1 = :user1 AND f.user2 = :user2) OR " +
            "(f.user1 = :user2 AND f.user2 = :user1)")
    Optional<Friend> findByUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "JOIN Friend f ON (f.user1.id = :userId AND f.user2.id = u.id) " +
            "                OR (f.user2.id = :userId AND f.user1.id = u.id) " +
            "WHERE u.isDeleted = false " +
            "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY u.username, u.nickname")
    List<User> searchFriendUsers(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Query("SELECT f.user2.id FROM Friend f WHERE f.user1.id = :userId " +
            "UNION " +
            "SELECT f.user1.id FROM Friend f WHERE f.user2.id = :userId")
    List<Long> findFriendIdsByUserId(@Param("userId") Long userId);

}
