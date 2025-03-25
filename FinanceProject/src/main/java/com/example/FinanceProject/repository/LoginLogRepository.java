package com.example.FinanceProject.repository;

import com.example.FinanceProject.entity.LoginLog;
import com.example.FinanceProject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
    
    List<LoginLog> findByUser(User user);
    
    List<LoginLog> findByUsernameAndLoginTimeBetween(String username, LocalDateTime start, LocalDateTime end);
    
    List<LoginLog> findBySuccessful(boolean successful);
}