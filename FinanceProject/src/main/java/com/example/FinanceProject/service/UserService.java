package com.example.FinanceProject.service;

import com.example.FinanceProject.PendingUser;
import com.example.FinanceProject.User;
import com.example.FinanceProject.repository.PendingUserRepo;
import com.example.FinanceProject.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PendingUserRepo pendingUserRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(String username, String password, String role, String firstName, String lastName, String address, String dob, String email) {
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
        // Set status to "ACCEPTED" so the user can log in
        user.setStatus("ACCEPTED");
        userRepo.save(user);
    }

    public void registerPendingUser(String username, String password, String role, String firstName, String lastName, String address, String dob, String email) {
        String hashedPassword = passwordEncoder.encode(password);
        PendingUser pendingUser = new PendingUser();
        pendingUser.setUsername(username);
        pendingUser.setPassword(hashedPassword);
        pendingUser.setRole(role);
        pendingUser.setFirstName(firstName);
        pendingUser.setLastName(lastName);
        pendingUser.setAddress(address);
        pendingUser.setDob(dob);
        pendingUser.setEmail(email);
        pendingUser.setStatus("PENDING");
        pendingUserRepo.save(pendingUser);
    }

    // Retrieve all pending registrations
    public Iterable<PendingUser> getAllPendingUsers() {
        return pendingUserRepo.findAll();
    }

    public void acceptPendingUser(Long pendingUserId) {
        PendingUser pendingUser = pendingUserRepo.findById(pendingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Pending user not found"));
    
        // Create a new active user without rehashing the already hashed password.
        User user = new User();
        user.setUsername(pendingUser.getUsername());
        user.setPassword(pendingUser.getPassword());  // Use the hash as-is.
        user.setRole(pendingUser.getRole());
        user.setFirstName(pendingUser.getFirstName());
        user.setLastName(pendingUser.getLastName());
        user.setAddress(pendingUser.getAddress());
        user.setDob(pendingUser.getDob());
        user.setEmail(pendingUser.getEmail());
        user.setStatus("ACCEPTED");  // Explicitly set the status.
        
        userRepo.save(user);
        pendingUserRepo.deleteById(pendingUserId);
    }
    
    

    // Deny a pending user: simply remove the pending record
    public void denyPendingUser(Long pendingUserId) {
        pendingUserRepo.deleteById(pendingUserId);
    }

    // Existing method to get all active users (for admin view)
    public Iterable<User> getAllUsers() {
        return userRepo.findAll();
    }

    // Suspend the user
    public void suspendUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("SUSPENDED");
        userRepo.save(user);
    }

    // Unsuspend the user
    public void unsuspendUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("ACCEPTED");
        userRepo.save(user);
    }

    public User getUserById(Long id) {
        Optional<User> userOptional = userRepo.findById(id);
        if (!userOptional.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        return userOptional.get();
    }

    public void updateUser(Long id, String username, String role, String firstName, 
                          String lastName, String address, String dob, String email) {
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
