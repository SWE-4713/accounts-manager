package com.example.FinanceProject.repository;

import com.example.FinanceProject.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PendingUserRepo extends JpaRepository<PendingUser, Long> {
    Optional<PendingUser> findByUsername(String username);
    Optional<PendingUser> findByEmail(String username);
}