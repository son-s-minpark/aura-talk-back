package com.sonsminpark.auratalkback.domain.chat.repository;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
            "LEFT JOIN FETCH cr.owner o " +
            "LEFT JOIN FETCH o.userProfileImage " +
            "JOIN ChatRoomUser cru ON cr.id = cru.chatRoom.id " +
            "WHERE cru.user.id = :userId " +
            "ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> findActiveByUserIdWithOwner(@Param("userId") Long userId);

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
            "LEFT JOIN FETCH cr.owner o " +
            "LEFT JOIN FETCH o.userProfileImage " +
            "JOIN ChatRoomUser cru ON cr.id = cru.chatRoom.id " +
            "WHERE cru.user.id = :userId")
    Page<ChatRoom> findActiveByUserIdWithOwnerPaging(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT cr FROM ChatRoom cr " +
            "JOIN ChatRoomUser cru1 ON cr.id = cru1.chatRoom.id " +
            "JOIN ChatRoomUser cru2 ON cr.id = cru2.chatRoom.id " +
            "WHERE cr.type = :type AND cr.isActive = true " +
            "AND cru1.user.id = :user1Id AND cru2.user.id = :user2Id " +
            "AND cru1.user.id != cru2.user.id")
    Optional<ChatRoom> findOneToOneChatRoom(
            @Param("type") ChatRoomType type,
            @Param("user1Id") Long user1Id,
            @Param("user2Id") Long user2Id);

    Optional<ChatRoom> findByInviteCodeAndIsActiveTrue(String inviteCode);

    @Query("SELECT CASE WHEN COUNT(cru) > 0 THEN true ELSE false END " +
            "FROM ChatRoomUser cru " +
            "WHERE cru.chatRoom.id = :chatRoomId AND cru.user.id = :userId")
    boolean isUserInChatRoom(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr WHERE LOWER(cr.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND cr.isActive = true")
    List<ChatRoom> searchByName(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
            "JOIN ChatRoomUser cru ON cr.id = cru.chatRoom.id " +
            "LEFT JOIN ChatRoomUser cru2 ON cr.id = cru2.chatRoom.id " +
            "LEFT JOIN cru2.user u ON u.id = cru2.user.id " +
            "WHERE cru.user.id = :userId AND cr.isActive = true " +
            "AND (LOWER(cr.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> searchByNameAndUserId(@Param("keyword") String keyword, @Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr " +
            "LEFT JOIN FETCH cr.owner o " +
            "LEFT JOIN FETCH o.userProfileImage " +
            "WHERE cr.id = :chatRoomId")
    Optional<ChatRoom> findByIdWithOwner(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cr FROM ChatRoom cr " +
            "LEFT JOIN FETCH cr.bannedUsers bu " +
            "LEFT JOIN FETCH bu.userProfileImage " +
            "WHERE cr.id = :chatRoomId")
    Optional<ChatRoom> findByIdWithBannedUsers(@Param("chatRoomId") Long chatRoomId);
}