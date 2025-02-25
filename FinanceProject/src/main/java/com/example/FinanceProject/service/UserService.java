package com.example.FinanceProject.service;

import com.example.FinanceProject.User;
import com.example.FinanceProject.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(String username, String password, String role) {
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

        // Save the user in the database
        userRepo.save(user);
    }
}
