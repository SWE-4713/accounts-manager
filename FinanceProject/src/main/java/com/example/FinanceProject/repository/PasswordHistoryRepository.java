package com.example.FinanceProject.repository;

import com.example.FinanceProject.PasswordHistory;
import com.example.FinanceProject.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
       List<PasswordHistory> findByUserOrderByCreatedAtDesc(User user);
       
       // Optional: Method to find only a certain number of most recent passwords
       List<PasswordHistory> findTopNByUserOrderByCreatedAtDesc(User user, Pageable pageable);
   }