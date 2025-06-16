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

    List<FriendRequest> findByRequester(User requester);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.recipient = :recipient " +
            "AND fr.requester NOT IN " + "(SELECT fb.blocked FROM FriendBlock fb WHERE fb.blocker = :recipient)")
    List<FriendRequest> findByRecipientExcludingBlocked(@Param("recipient") User recipient);

}
