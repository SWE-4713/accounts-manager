package com.example.FinanceProject.service;

import com.example.FinanceProject.PasswordHistory;
import com.example.FinanceProject.PasswordResetToken;
import com.example.FinanceProject.PendingUser;
import com.example.FinanceProject.dto.RegistrationRequest;
import com.example.FinanceProject.User;
import com.example.FinanceProject.repository.PasswordHistoryRepository;
import com.example.FinanceProject.repository.PasswordResetTokenRepository;
import com.example.FinanceProject.repository.PendingUserRepo;
import com.example.FinanceProject.repository.UserRepo;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PendingUserRepo pendingUserRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordExpirationService passwordExpirationService;

    // Number of previous passwords to check against
    private static final int PASSWORD_HISTORY_LIMIT = 5;

    public boolean checkPasswordExpiration(User user) {
        return passwordExpirationService.isPasswordExpired(user);
    }

    /**
     * Finds a user by their username
     * @param username The username to search for
     * @return The user if found, or null if no user exists with that username
     */
    public User findUserByUsername(String username) {
        Optional<User> userOptional = userRepo.findByUsername(username);
        return userOptional.orElse(null);
    }

    /**
     * Initiates the forgot password process by generating a token and sending an email
     * @param email The email address of the user requesting password reset
     * @return true if email was found and reset email was sent, false otherwise
     */
    @Transactional
    public boolean initiatePasswordReset(String email) {
        Optional<User> userOptional = userRepo.findByEmail(email);


        if (userOptional.isPresent()) {
            User user = userOptional.get();
            passwordResetTokenRepository.deleteByUser(user);
            String token = generatePasswordResetToken(user);
            try {
                sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
                return true;
            } catch (MessagingException e) {
                // Log the error
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * Generates and stores a password reset token for a user
     * @param user The user requesting password reset
     * @return The generated token
     */
    private String generatePasswordResetToken(User user) {
        // First invalidate any existing tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate a new token
        String token = UUID.randomUUID().toString();

        // Create token with expiration (typically 24 hours)
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(24));

        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    /**
     * Validates a password reset token
     * @param token The token to validate
     * @return The user associated with the valid token, or null if invalid or expired
     */
    public User validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);

        if (tokenOptional.isPresent()) {
            PasswordResetToken resetToken = tokenOptional.get();

            // Check if token is expired
            if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                passwordResetTokenRepository.delete(resetToken);
                return null;
            }

            return resetToken.getUser();
        }

        return null;
    }

    /**
     * Encodes a password using the application's password encoder
     *
     * @param rawPassword The plain text password to encode
     * @return The encoded password string
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Completes the password reset process by updating the user's password
     * @param token The reset token
     * @param newPassword The new password
     * @return true if password was successfully reset, false otherwise
     */
    public boolean resetPassword(String token, String newPassword) {
        User user = validatePasswordResetToken(token);
        if (user == null) {
            return false;
        }

        if (isPasswordPreviouslyUsed(user, newPassword)) {
            throw new PasswordReusedException("Password has been used previously. Please choose a different password.");
        }


        user.setPassword(passwordEncoder.encode(newPassword));
        passwordExpirationService.updatePasswordDate(user); // Update password date
        user.setLockTime(new Date(0));
        user.setPasswordUpdateDate(new Date());
        userRepo.save(user);

        // Save the current password to history before changing it
        savePasswordToHistory(user, user.getPassword());

        try {
            sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
        } catch (MessagingException e) {
            // Log exception
        }

        return true;
    }

    private void savePasswordToHistory(User user, String passwordHash) {
        PasswordHistory history = new PasswordHistory(user, passwordHash);
        passwordHistoryRepository.save(history);

        // Optional: Maintain only a certain number of password history entries
        // This helps to manage database growth
        List<PasswordHistory> histories = passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user);
        if (histories.size() > PASSWORD_HISTORY_LIMIT) {
            // Remove older entries beyond the limit
            for (int i = PASSWORD_HISTORY_LIMIT; i < histories.size(); i++) {
                passwordHistoryRepository.delete(histories.get(i));
            }
        }
    }

    public class PasswordReusedException extends RuntimeException {
        public PasswordReusedException(String message) {
            super(message);
        }
    }

    private boolean isPasswordPreviouslyUsed(User user, String newPassword) {
        // Get the user's password history
        List<PasswordHistory> passwordHistories = passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user);

        // Check if the new password matches any previous passwords
        for (PasswordHistory history : passwordHistories) {
            if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a password reset email to the user
     */
    private void sendPasswordResetEmail(String userEmail, String firstName, String token) throws MessagingException {
        String subject = "Password Reset Request";
        String resetUrl = "http://localhost:8080/reset-password?token=" + token; // Adjust to your application's URL

        String content =
                "<div style='font-family: Arial, sans-serif; max-width: 600px;'>" +
                        "<h2>Password Reset Request</h2>" +
                        "<p>Hello " + firstName + ",</p>" +
                        "<p>We received a request to reset your password. If you didn't make this request, you can ignore this email.</p>" +
                        "<p>To reset your password, click the link below (valid for 24 hours):</p>" +
                        "<p><a href='" + resetUrl + "' style='display: inline-block; background-color: #4CAF50; color: white; padding: 10px 15px; " +
                        "text-decoration: none; border-radius: 4px;'>Reset Password</a></p>" +
                        "<p>If the button doesn't work, copy and paste this URL into your browser:</p>" +
                        "<p>" + resetUrl + "</p>" +
                        "<p>Thank you,<br>The Finance Project Team</p>" +
                        "</div>";

        emailService.sendHtmlEmail(userEmail, subject, content);
    }

    /**
     * Sends a confirmation email when password has been changed
     */
    private void sendPasswordChangedEmail(String userEmail, String firstName) throws MessagingException {
        String subject = "Password Successfully Changed";

        String content =
                "<div style='font-family: Arial, sans-serif; max-width: 600px;'>" +
                        "<h2>Password Changed Successfully</h2>" +
                        "<p>Hello " + firstName + ",</p>" +
                        "<p>Your password has been successfully changed.</p>" +
                        "<p>If you did not make this change, please contact our support team immediately.</p>" +
                        "<p>Thank you,<br>The Finance Project Team</p>" +
                        "</div>";

        emailService.sendHtmlEmail(userEmail, subject, content);
    }

    public void registerNewUser(RegistrationRequest request) {
        if (userRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(hashedPassword);
        // You'll need to get these values from the RegistrationRequest or
        // set defaults.
        user.setRole("USER"); // Or get from RegistrationRequest
        user.setFirstName("N/A"); // Or get from RegistrationRequest
        user.setLastName("N/A"); // Or get from RegistrationRequest
        user.setAddress("N/A"); // Or get from RegistrationRequest
        user.setDob("N/A"); // Or get from RegistrationRequest
        user.setEmail("N/A"); // Or get from RegistrationRequest
        user.setStatus("ACCEPTED");

        user.setLockTime(new Date(0));
        user.setPasswordUpdateDate(new Date());
        userRepo.save(user);
        savePasswordToHistory(user, user.getPassword());
    }

    // Notify all admin users about a new pending registration
    public void notifyAdminsOfNewPendingUser(String username, String firstName, String lastName, String email, String role) {
        // Find all active admin users
        List<User> adminUsers = userRepo.findByRoleAndStatusNot("ROLE_ADMIN", "SUSPENDED");
        
        if (adminUsers.isEmpty()) {
            System.out.println("No admin users found, sending to default admin email");
            try {
                // Send to a default admin email if no admin users are in the system
                sendAdminNotificationEmail("syguy2003@gmail.com", username, firstName, lastName, email, role);
            } catch (Exception e) {
                System.err.println("Failed to send to default admin email: " + e.getMessage());
            }
            return;
        }
        
        // Send email to each admin
        for (User admin : adminUsers) {
            if (admin.getEmail() != null && !admin.getEmail().isEmpty() && !admin.getEmail().equals("N/A")) {
                try {
                    sendAdminNotificationEmail(admin.getEmail(), username, firstName, lastName, email, role);
                    System.out.println("Notification email sent to admin: " + admin.getEmail());
                } catch (Exception e) {
                    System.err.println("Failed to send notification email to admin " + admin.getEmail() + ": " + e.getMessage());
                }
            }
        }
    }
    
    // Helper method to send the HTML notification email
    private void sendAdminNotificationEmail(String adminEmail, String username, String firstName, 
                                           String lastName, String userEmail, String role) throws MessagingException {
        String subject = "New User Registration Pending Approval";
        String htmlBody = 
            "<html>" +
            "<body style='font-family: Arial, sans-serif;'>" +
            "<div style='background-color: #f5f5f5; padding: 20px;'>" +
            "<h2 style='color: #007bff;'>New User Registration</h2>" +
            "<p>A new user has registered and is pending approval:</p>" +
            "<table style='width: 100%; border-collapse: collapse;'>" +
            "<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>Username:</td><td style='padding: 8px; border: 1px solid #ddd;'>" + username + "</td></tr>" +
            "<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>Name:</td><td style='padding: 8px; border: 1px solid #ddd;'>" + firstName + " " + lastName + "</td></tr>" +
            "<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>Email:</td><td style='padding: 8px; border: 1px solid #ddd;'>" + userEmail + "</td></tr>" +
            "<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>Role:</td><td style='padding: 8px; border: 1px solid #ddd;'>" + role + "</td></tr>" +
            "</table>" +
            "<p style='margin-top: 20px;'>Please review this registration in the admin dashboard.</p>" +
            "</div>" +
            "</body>" +
            "</html>";
            
        emailService.sendHtmlEmail(adminEmail, subject, htmlBody);
    }

    // Method for registering pending users - now with admin notification
    public void registerPendingUser(
            String role,
            String firstName,
            String lastName,
            String address,
            String dob,
            String email) {
        if (pendingUserRepo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }


        PendingUser pendingUser = new PendingUser();
        // Generate username based on first initial, last name, and current date
        LocalDateTime now = LocalDateTime.now();
        String month = String.format("%02d", now.getMonthValue());
        String year = String.format("%02d", now.getYear() % 100);

        // Create username: first initial + last name + month + year
        String username = (firstName.substring(0, 1) + lastName + month + year).toLowerCase();

        pendingUser.setUsername(username);
        pendingUser.setRole(role);
        pendingUser.setFirstName(firstName);
        pendingUser.setLastName(lastName);
        pendingUser.setAddress(address);
        pendingUser.setDob(dob);
        pendingUser.setEmail(email);
        pendingUser.setStatus("PENDING");
        pendingUserRepo.save(pendingUser);
        
        // After successfully saving the pending user, notify all admins
        try {
            notifyAdminsOfNewPendingUser(username, firstName, lastName, email, role);
        } catch (Exception e) {
            // Log the error but don't prevent the registration from completing
            System.err.println("Error sending admin notifications: " + e.getMessage());
        }
    }

    public void registerSetUser(
            String username,
            String password,
            String role,
            String firstName,
            String lastName,
            String address,
            String dob,
            String email) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setRole(role);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAddress(address);
        user.setDob(dob);
        user.setEmail(email);
        user.setStatus("ACCEPTED");
        user.setLockTime(new Date(0));
        user.setPasswordUpdateDate(new Date());
        userRepo.save(user);
        savePasswordToHistory(user, user.getPassword());
    }

    /**
     * Saves or updates a user in the database
     *
     * @param user The user entity to save
     * @return The saved user entity with any generated IDs and default values
     */
    public User saveUser(User user) {
        return userRepo.save(user);
    }

    // Retrieve all pending registrations
    public Iterable<PendingUser> getAllPendingUsers() {
        return pendingUserRepo.findAll();
    }

    public void acceptPendingUser(Long pendingUserId) {
        PendingUser pendingUser = pendingUserRepo
                .findById(pendingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Pending user not found"));


        // Generate a random password
        String password = generateRandomPassword();
        String encodedPassword = encodePassword(password);

        // Create a new active user without rehashing the already hashed password
        User user = new User();
        user.setUsername(pendingUser.getUsername());
        user.setPassword(encodedPassword);
        user.setRole(pendingUser.getRole());
        user.setFirstName(pendingUser.getFirstName());
        user.setLastName(pendingUser.getLastName());
        user.setAddress(pendingUser.getAddress());
        user.setDob(pendingUser.getDob());
        user.setEmail(pendingUser.getEmail());
        user.setStatus("ACCEPTED"); // Explicitly set the status

        user.setLockTime(new Date(0));
        user.setPasswordUpdateDate(new Date());
        userRepo.save(user);
        savePasswordToHistory(user, user.getPassword());


        // Send email with credentials to the user
        sendCredentialsEmail(user.getEmail(), user.getUsername(), password);

        pendingUserRepo.deleteById(pendingUserId);

        // Notify the user that their account has been approved (optional)
        try {
//            sendAccountApprovalEmail(user.getEmail(), user.getFirstName());
            System.out.println("Accept user");
        } catch (Exception e) {
            System.err.println("Failed to send approval email: " + e.getMessage());
        }
    }

    /**
     * Sends an email with the generated username and password
     */
    private void sendCredentialsEmail(String email, String username, String password) {
        try {
            String subject = "Your Account Has Been Approved";
            String content = "Dear User,\n\n" +
                    "Your account has been approved. Here are your login credentials:\n\n" +
                    "Username: " + username + "\n" +
                    "Password: " + password + "\n\n" +
                    "Please login using these credentials and change your password immediately.\n\n" +
                    "Regards,\nThe Finance Project Team";

            emailService.sendSimpleEmail(email, subject, content);
        } catch (Exception e) {
            // Log the exception but don't prevent the user creation
            System.err.println("Failed to send credentials email: " + e.getMessage());
        }
    }

    /**
     * Generates a random password
     */
    private String generateRandomPassword() {
        // Generate a random password with 10 characters
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            int index = (int)(chars.length() * Math.random());
            password.append(chars.charAt(index));
        }

        return password.toString();
    }

    // Send account approval email to the user
    private void sendAccountApprovalEmail(String userEmail, String firstName) {
        if (userEmail == null || userEmail.isEmpty() || userEmail.equals("N/A")) {
            return; // Don't try to send if no valid email
        }
        
        String subject = "Your Account Has Been Approved";
        String body = "Dear " + firstName + ",\n\n" +
                      "Your account registration has been approved. You can now log in to the system.\n\n" +
                      "Thank you for joining our platform.\n\n" +
                      "Best regards,\n" +
                      "The Finance Project Team";
        
        try {
            emailService.sendSimpleEmail(userEmail, subject, body);
        } catch (Exception e) {
            System.err.println("Error sending approval email: " + e.getMessage());
        }
    }

    // Deny a pending user: simply remove the pending record
    public void denyPendingUser(Long pendingUserId) {
        // Get the pending user to access their email before deleting
        PendingUser pendingUser = pendingUserRepo.findById(pendingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Pending user not found"));
                
        // Notify the user that their registration was denied (optional)
        try {
            sendRegistrationDeniedEmail(pendingUser.getEmail(), pendingUser.getFirstName());
        } catch (Exception e) {
            System.err.println("Failed to send denial email: " + e.getMessage());
        }
        
        pendingUserRepo.deleteById(pendingUserId);
    }
    
    // Send registration denial email to the user
    private void sendRegistrationDeniedEmail(String userEmail, String firstName) {
        if (userEmail == null || userEmail.isEmpty() || userEmail.equals("N/A")) {
            return; // Don't try to send if no valid email
        }
        
        String subject = "Regarding Your Account Registration";
        String body = "Dear " + firstName + ",\n\n" +
                      "We regret to inform you that your account registration request has been denied.\n\n" +
                      "If you believe this is an error or would like more information, please contact our support team.\n\n" +
                      "Best regards,\n" +
                      "The Finance Project Team";
        
        try {
            emailService.sendSimpleEmail(userEmail, subject, body);
        } catch (Exception e) {
            System.err.println("Error sending denial email: " + e.getMessage());
        }
    }

    // Existing method to get all active users (for admin view)
    public Iterable<User> getAllUsers() {
        return userRepo.findAll();
    }

    // Suspend the user
    public void suspendUser(Long id) {
        User user = userRepo
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("SUSPENDED");
        userRepo.save(user);
        
        // Notify the user of account suspension
        try {
            sendAccountSuspensionEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            System.err.println("Failed to send suspension notification: " + e.getMessage());
        }
    }
    
    // Send account suspension notification
    private void sendAccountSuspensionEmail(String userEmail, String firstName) {
        if (userEmail == null || userEmail.isEmpty() || userEmail.equals("N/A")) {
            return;
        }
        
        String subject = "Your Account Has Been Suspended";
        String body = "Dear " + firstName + ",\n\n" +
                      "Your account has been suspended. If you believe this is an error or would like more information, " +
                      "please contact our support team.\n\n" +
                      "Best regards,\n" +
                      "The Finance Project Team";
        
        try {
            emailService.sendSimpleEmail(userEmail, subject, body);
        } catch (Exception e) {
            System.err.println("Error sending suspension email: " + e.getMessage());
        }
    }

    // Unsuspend the user
    public void unsuspendUser(Long id) {
        User user = userRepo
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("ACCEPTED");
        userRepo.save(user);
        
        // Notify the user that their account has been unsuspended
        try {
            sendAccountReinstatedEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            System.err.println("Failed to send reinstatement notification: " + e.getMessage());
        }
    }
    
    // Send account reinstatement notification
    private void sendAccountReinstatedEmail(String userEmail, String firstName) {
        if (userEmail == null || userEmail.isEmpty() || userEmail.equals("N/A")) {
            return;
        }
        
        String subject = "Your Account Has Been Reinstated";
        String body = "Dear " + firstName + ",\n\n" +
                      "Your account has been reinstated and is now active. You can now log in to the system.\n\n" +
                      "Thank you for your patience.\n\n" +
                      "Best regards,\n" +
                      "The Finance Project Team";
        
        try {
            emailService.sendSimpleEmail(userEmail, subject, body);
        } catch (Exception e) {
            System.err.println("Error sending reinstatement email: " + e.getMessage());
        }
    }

    public User getUserById(Long id) {
        Optional<User> userOptional = userRepo.findById(id);
        if (!userOptional.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        return userOptional.get();
    }

    public void updateUser(
            Long id,
            String username,
            String role,
            String firstName,
            String lastName,
            String address,
            String dob,
            String email) {
        // Get the existing user
        User existingUser = getUserById(id);

        // Check if username is being changed and if the new username already exists
        if (!existingUser.getUsername().equals(username) &&
                userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Update user fields
        existingUser.setUsername(username);
        existingUser.setRole(role);
        existingUser.setFirstName(firstName);
        existingUser.setLastName(lastName);
        existingUser.setAddress(address);
        existingUser.setDob(dob);
        existingUser.setEmail(email);

        // Save the updated user
        userRepo.save(existingUser);
    }
}
