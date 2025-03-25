package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class PasswordExpirationNotificationService {

    private static final int NOTIFICATION_DAYS_BEFORE_EXPIRY = 3;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private EmailService emailService;

    /**
     * Scheduled task that runs daily to check for passwords that will expire in 3 days
     * and sends notification emails to those users
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void checkAndSendExpirationNotifications() {
        // Find users whose passwords will expire in 3 days
        List<User> users = userRepo.findAll();
        Date currentDate = new Date();
        
        for (User user : users) {
            if (user.getPasswordUpdateDate() != null && user.getEmail() != null) {
                // Calculate the expiration date
                Calendar expiryCalendar = Calendar.getInstance();
                expiryCalendar.setTime(user.getPasswordUpdateDate());
                expiryCalendar.add(Calendar.DAY_OF_MONTH, PasswordExpirationService.PASSWORD_EXPIRATION_DAYS);
                
                // Calculate the notification date (3 days before expiration)
                Calendar notificationCalendar = Calendar.getInstance();
                notificationCalendar.setTime(expiryCalendar.getTime());
                notificationCalendar.add(Calendar.DAY_OF_MONTH, -NOTIFICATION_DAYS_BEFORE_EXPIRY);
                
                // If today is the day to send notification (or if we're slightly past it but notification hasn't been sent)
                if (isSameDay(currentDate, notificationCalendar.getTime()) || 
                    (currentDate.after(notificationCalendar.getTime()) && 
                     currentDate.before(expiryCalendar.getTime()) && 
                     !user.isPasswordExpiryNotificationSent())) {
                    
                    sendPasswordExpirationNotification(user, expiryCalendar.getTime());
                    
                    // Mark that notification has been sent
                    user.setPasswordExpiryNotificationSent(true);
                    userRepo.save(user);
                }
                
                // Reset the notification flag if password has been changed
                if (currentDate.before(notificationCalendar.getTime()) && user.isPasswordExpiryNotificationSent()) {
                    user.setPasswordExpiryNotificationSent(false);
                    userRepo.save(user);
                }
            }
        }
    }
    
    /**
     * Send password expiration notification email to a user
     * @param user The user to notify
     * @param expiryDate The date when the password will expire
     */
    private void sendPasswordExpirationNotification(User user, Date expiryDate) {
        String subject = "Your password will expire soon";
        String body = String.format(
            "Dear %s,\n\nYour account password will expire on %s. " +
            "Please log in to the system and change your password before that date " +
            "to maintain access to your account.\n\nRegards,\nAtlas Finances Team", 
            user.getFirstName(), expiryDate.toString());
        
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }
    
    /**
     * Utility method to check if two dates fall on the same day
     */
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}