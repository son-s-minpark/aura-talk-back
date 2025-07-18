package com.sonsminpark.auratalkback.domain.user.repository;

import com.sonsminpark.auratalkback.domain.user.entity.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {
}
