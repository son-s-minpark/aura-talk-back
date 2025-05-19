package com.sonsminpark.auratalkback.domain.chat.repository;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatInvitation;
import com.sonsminpark.auratalkback.domain.chat.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatInvitationRepository extends JpaRepository<ChatInvitation, Long> {

    List<ChatInvitation> findByInviteeIdAndStatus(Long inviteeId, InvitationStatus status);

    @Query("SELECT ci FROM ChatInvitation ci WHERE ci.chatRoom.id = :chatroomId AND ci.invitee.id = :inviteeId AND ci.status = :status")
    Optional<ChatInvitation> findByChatRoomIdAndInviteeIdAndStatus(
            @Param("chatroomId") Long chatroomId,
            @Param("inviteeId") Long inviteeId,
            @Param("status") InvitationStatus status);

    List<ChatInvitation> findByInviterId(Long inviterId);
}