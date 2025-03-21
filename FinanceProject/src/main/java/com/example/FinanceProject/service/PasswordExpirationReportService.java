package com.example.FinanceProject.service;

import com.example.FinanceProject.User;
import com.example.FinanceProject.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class PasswordExpirationReportService {

    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private EmailService emailService;

    /**
     * Find all users with expired passwords
     */
    public List<User> findUsersWithExpiredPasswords() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -PasswordExpirationService.PASSWORD_EXPIRATION_DAYS);
        Date expirationThreshold = calendar.getTime();
        
        return userRepo.findByPasswordLastChangedBefore(expirationThreshold);
    }
    
    /**
     * Generate and send a report of expired passwords to admin
     */
    public void sendExpiredPasswordsReport() {
        List<User> usersWithExpiredPasswords = findUsersWithExpiredPasswords();
        
        if (usersWithExpiredPasswords.isEmpty()) {
            return;
        }
        
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("Expired Passwords Report\n\n");
        reportBuilder.append("Total users with expired passwords: ").append(usersWithExpiredPasswords.size()).append("\n\n");
        
        reportBuilder.append("| Username | Email | Last Password Change | Days Expired |\n");
        reportBuilder.append("|----------|-------|---------------------|-------------|\n");
        
        Calendar now = Calendar.getInstance();
        Date currentDate = now.getTime();
        
        for (User user : usersWithExpiredPasswords) {
            long diffInMillies = currentDate.getTime() - user.getPasswordLastChanged().getTime();
            long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);
            
            reportBuilder.append("| ")
                    .append(user.getUsername()).append(" | ")
                    .append(user.getEmail()).append(" | ")
                    .append(user.getPasswordLastChanged()).append(" | ")
                    .append(diffInDays).append(" |\n");
        }

        List<User> adminUsers = userRepo.findByRoleAndStatusNot("ROLE_ADMIN", "SUSPENDED");

        for (User admin : adminUsers) {
            if (admin.getEmail() != null && !admin.getEmail().isEmpty() && !admin.getEmail().equals("N/A")) {
                try {
                    emailService.sendSimpleEmail(admin.getEmail(), "Expired Passwords Report", reportBuilder.toString() + "\n\n--");
                } catch (Exception e) {
                    System.err.println("Failed to send notification email to admin " + admin.getEmail() + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Schedule this to run weekly or as needed
     */
    @Scheduled(cron = "0 0 9 ? * MON") // Run at 9:00 AM every Monday
    public void scheduledExpiredPasswordsReport() {
        sendExpiredPasswordsReport();
    }
}