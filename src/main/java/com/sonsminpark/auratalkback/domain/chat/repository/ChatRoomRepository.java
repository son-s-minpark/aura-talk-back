package com.sonsminpark.auratalkback.domain.chat.repository;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.users u WHERE u.id = :userId AND cr.isActive = true ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = :type AND cr.isActive = true AND :user1 MEMBER OF cr.users AND :user2 MEMBER OF cr.users")
    Optional<ChatRoom> findOneToOneChatRoom(@Param("type") ChatRoomType type, @Param("user1") User user1, @Param("user2") User user2);

    Optional<ChatRoom> findByInviteCodeAndIsActiveTrue(String inviteCode);

    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END FROM ChatRoom cr JOIN cr.users u WHERE cr.id = :chatRoomId AND u.id = :userId AND cr.isActive = true")
    boolean isUserInChatRoom(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr WHERE LOWER(cr.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND cr.isActive = true")
    List<ChatRoom> searchByName(@Param("keyword") String keyword);
}