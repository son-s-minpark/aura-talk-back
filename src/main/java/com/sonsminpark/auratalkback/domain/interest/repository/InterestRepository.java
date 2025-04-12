package com.sonsminpark.auratalkback.domain.interest.repository;

import com.sonsminpark.auratalkback.domain.interest.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterestRepository extends JpaRepository<Interest, Long> {

    Optional<Interest> findByName(String name);

    List<Interest> findByCategory(String category);

    @Query("SELECT DISTINCT i.category FROM Interest i ORDER BY i.category")
    List<String> findAllCategories();
}