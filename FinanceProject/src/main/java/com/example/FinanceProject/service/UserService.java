package com.example.FinanceProject.service;

import com.example.FinanceProject.User;
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
    private PasswordEncoder passwordEncoder;

    public void registerUser(String username, String password, String role, String firstName, String lastName, String address, String dob, String email) {
        // Check if the user already exists
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Hash the password
        String hashedPassword = passwordEncoder.encode(password);

        // Create a new user entity
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setRole(role);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAddress(address);
        user.setDob(dob);
        user.setEmail(email);

        // Save the user in the database
        userRepo.save(user);
    }

    public Iterable<User> getAllUsers() {
        return userRepo.findAll();
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
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
