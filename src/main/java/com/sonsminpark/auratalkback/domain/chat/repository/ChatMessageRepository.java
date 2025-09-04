package com.sonsminpark.auratalkback.domain.chat.repository;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT cm FROM ChatMessage cm " +
            "LEFT JOIN FETCH cm.sender s " +
            "LEFT JOIN FETCH s.userProfileImage " +
            "WHERE cm.chatRoom.id = :chatRoomId AND cm.isDeleted = false")
    Page<ChatMessage> findByChatRoomIdWithSender(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.id = :messageId AND cm.sender.id = :senderId AND cm.isDeleted = false")
    Optional<ChatMessage> findByIdAndSenderId(@Param("messageId") Long messageId, @Param("senderId") Long senderId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.isDeleted = false")
    long countByChatRoomIdAndIsDeletedFalse(@Param("chatRoomId") Long chatRoomId);

    void deleteAllByChatRoomId(Long chatRoomId);
}