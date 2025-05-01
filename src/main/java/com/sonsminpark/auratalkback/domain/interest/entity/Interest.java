package com.sonsminpark.auratalkback.domain.interest.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "interests")
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String category;

    // 자동 저장을 위한 정적 메서드
    public static List<Interest> createDefaultInterests() {
        List<Interest> interests = new ArrayList<>();

        // 취미
        interests.add(Interest.builder().name("게임").category("취미").build());
        interests.add(Interest.builder().name("요리").category("취미").build());
        interests.add(Interest.builder().name("댄스").category("취미").build());
        interests.add(Interest.builder().name("웹서핑").category("취미").build());
        interests.add(Interest.builder().name("프로그래밍").category("취미").build());

        // 자기개발
        interests.add(Interest.builder().name("독서").category("자기개발").build());
        interests.add(Interest.builder().name("운동").category("자기개발").build());
        interests.add(Interest.builder().name("미라클모닝").category("자기개발").build());
        interests.add(Interest.builder().name("공부").category("자기개발").build());

        // 일상
        interests.add(Interest.builder().name("코디").category("일상").build());
        interests.add(Interest.builder().name("메뉴 추천").category("일상").build());
        interests.add(Interest.builder().name("챌린지").category("일상").build());

        return interests;
    }
}