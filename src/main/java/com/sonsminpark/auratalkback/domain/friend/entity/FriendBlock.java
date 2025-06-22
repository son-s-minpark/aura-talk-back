package com.sonsminpark.auratalkback.domain.friend.entity;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "friend_blocks")
@Entity
public class FriendBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    private User blocked;
}
