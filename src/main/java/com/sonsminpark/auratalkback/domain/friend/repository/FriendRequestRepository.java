package com.sonsminpark.auratalkback.domain.friend.repository;

import com.sonsminpark.auratalkback.domain.friend.entity.FriendRequest;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findByRequesterAndRecipient(User requester, User recipient);

    @Query("SELECT r FROM FriendRequest fr " +
            "JOIN fr.recipient r " +
            "LEFT JOIN FETCH r.userProfileImage " +
            "WHERE fr.requester.id = :requesterId ")
    List<User> findByRequester(@Param("requesterId") Long requesterId);

    @Query("SELECT r FROM FriendRequest fr " +
            "JOIN fr.requester r " +
            "LEFT JOIN FETCH r.userProfileImage " +
            "WHERE fr.recipient.id = :recipientId " +
            "AND r NOT IN " +
            "(SELECT fb.blocked FROM FriendBlock fb WHERE fb.blocker = :recipient)")
    List<User> findByRecipientExcludingBlocked(@Param("recipientId") Long recipientId);

}
