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

        // 자기계발
        interests.add(Interest.builder().name("언어").category("자기계발").build());
        interests.add(Interest.builder().name("미라클모닝").category("자기계발").build());
        interests.add(Interest.builder().name("공부").category("자기계발").build());
        interests.add(Interest.builder().name("운동").category("자기계발").build());
        interests.add(Interest.builder().name("재테크").category("자기계발").build());
        interests.add(Interest.builder().name("필사").category("자기계발").build());
        interests.add(Interest.builder().name("취업/이직").category("자기계발").build());
        interests.add(Interest.builder().name("자격증").category("자기계발").build());

        // 취미
        interests.add(Interest.builder().name("게임").category("취미").build());
        interests.add(Interest.builder().name("요리").category("취미").build());
        interests.add(Interest.builder().name("독서").category("취미").build());
        interests.add(Interest.builder().name("베이킹").category("취미").build());
        interests.add(Interest.builder().name("사진").category("취미").build());
        interests.add(Interest.builder().name("덕질").category("취미").build());
        interests.add(Interest.builder().name("스포츠").category("취미").build());
        interests.add(Interest.builder().name("영화/드라마").category("취미").build());
        interests.add(Interest.builder().name("OTT").category("취미").build());
        interests.add(Interest.builder().name("뮤지컬/전시회 관람").category("취미").build());
        interests.add(Interest.builder().name("드로잉").category("취미").build());
        interests.add(Interest.builder().name("뜨개질").category("취미").build());
        interests.add(Interest.builder().name("주류 관련").category("취미").build());
        interests.add(Interest.builder().name("노래").category("취미").build());
        interests.add(Interest.builder().name("유튜브").category("취미").build());

        // 일상
        interests.add(Interest.builder().name("패션/코디").category("일상").build());
        interests.add(Interest.builder().name("챌린지").category("일상").build());
        interests.add(Interest.builder().name("연애").category("일상").build());
        interests.add(Interest.builder().name("다이어트").category("일상").build());
        interests.add(Interest.builder().name("맛집 추천").category("일상").build());
        interests.add(Interest.builder().name("쇼핑").category("일상").build());
        interests.add(Interest.builder().name("육아").category("일상").build());
        interests.add(Interest.builder().name("반려동물").category("일상").build());
        interests.add(Interest.builder().name("건강 관리").category("일상").build());
        interests.add(Interest.builder().name("여행").category("일상").build());
        interests.add(Interest.builder().name("고민상담").category("일상").build());
        interests.add(Interest.builder().name("시술/성형").category("일상").build());

        // 없음
        interests.add(Interest.builder().name("없음").category("없음").build());

        return interests;
    }
}