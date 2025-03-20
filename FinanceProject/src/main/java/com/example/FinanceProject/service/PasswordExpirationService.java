package com.example.FinanceProject.service;

import com.example.FinanceProject.User;
import com.example.FinanceProject.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.Calendar;
import java.util.Date;

@Service
public class PasswordExpirationService {

    private static final int PASSWORD_EXPIRATION_DAYS = 7;
    
    @Autowired
    private UserRepo userRepo;
    
    /**
     * Check if a user's password has expired
     * @param user The user to check
     * @return true if password has expired, false otherwise
     */
    public boolean isPasswordExpired(User user) {
        if (user.getPasswordUpdateDate() == null) {
            return true; // If no update date is set, consider it expired
        }
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(user.getPasswordUpdateDate());
        calendar.add(Calendar.DAY_OF_MONTH, PASSWORD_EXPIRATION_DAYS);
        
        Date expirationDate = calendar.getTime();
        Date currentDate = new Date();
        
        return currentDate.after(expirationDate);
    }
    
    /**
     * Updates the password update date for a user to current date
     * @param user The user whose password was updated
     */
    @Transactional
    public void updatePasswordDate(User user) {
        user.setPasswordUpdateDate(new Date());
        userRepo.save(user);
    }
}