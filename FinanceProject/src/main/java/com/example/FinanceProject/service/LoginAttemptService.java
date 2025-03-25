package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LoginAttemptService {
    public static final int MAX_FAILED_ATTEMPTS = 3;
    
    // Lock for 24 hours
    private static final long LOCK_TIME_DURATION = 5 * 60 * 1000;
    
    @Autowired
    private UserRepo userRepo;

    @Transactional
    public void increaseFailedAttempts(User user) {
        int newFailedAttempts = user.getFailedAttempt() + 1;
        userRepo.updateFailedAttempts(newFailedAttempts, user.getUsername());
    }

    @Transactional
    public void resetFailedAttempts(String username) {
        userRepo.updateFailedAttempts(0, username);
    }

    @Transactional
    public void lockUser(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(new Date());
        
        userRepo.save(user);
    }

    @Transactional
    public boolean unlockWhenTimeExpired(User user) {
        long lockTimeInMillis = user.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();
        
        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedAttempt(0);
            
            userRepo.save(user);
            
            return true;
        }
        
        return false;
    }
}