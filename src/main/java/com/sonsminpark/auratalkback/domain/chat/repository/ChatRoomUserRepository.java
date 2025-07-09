package com.sonsminpark.auratalkback.domain.chat.repository;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {

    Optional<ChatRoomUser> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    @Query("SELECT cru FROM ChatRoomUser cru WHERE cru.chatRoom.id = :chatRoomId AND cru.user.id = :userId")
    Optional<ChatRoomUser> findUserSettings(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    void deleteAllByChatRoomId(Long chatRoomId);

    @Query("SELECT cru FROM ChatRoomUser cru " +
            "JOIN FETCH cru.user u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE cru.chatRoom.id = :chatRoomId")
    List<ChatRoomUser> findAllByChatRoomIdWithUserAndProfile(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(cru) FROM ChatRoomUser cru WHERE cru.chatRoom.id = :chatRoomId")
    long countByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}