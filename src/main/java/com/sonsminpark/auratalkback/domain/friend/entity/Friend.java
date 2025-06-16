package com.sonsminpark.auratalkback.domain.friend.entity;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "friends")
@Entity
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id")
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id")
    private User user2;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static Friend create(User user1, User user2) {
        if (user1.getId() < user2.getId()) {
            return Friend.builder()
                    .user1(user1)
                    .user2(user2)
                    .build();
        }
        else {
            return Friend.builder()
                    .user1(user2)
                    .user2(user1)
                    .build();
        }
    }

}
