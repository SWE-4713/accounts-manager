package com.example.FinanceProject.repository;

import com.example.FinanceProject.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    List<User> findByRoleAndStatusNot(String role, String status);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedAttempt = ?1 WHERE u.username = ?2")
    public void updateFailedAttempts(int failedAttempts, String username);
}
