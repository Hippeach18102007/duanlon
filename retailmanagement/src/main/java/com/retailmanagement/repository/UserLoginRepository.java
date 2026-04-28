package com.retailmanagement.repository;

import com.retailmanagement.entity.UserLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLoginRepository
        extends JpaRepository<UserLogin, Long>,
                JpaSpecificationExecutor<UserLogin> {
    Optional<UserLogin>
    findTopByUser_IdAndSuccessTrueAndUpdatedAtIsNullOrderByCreatedAtDesc(Integer id);

    List<UserLogin> findByCreatedAtBetween(Instant from, Instant to);
}
