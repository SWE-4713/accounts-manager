package com.example.FinanceProject;

import com.example.FinanceProject.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {

        try {
            userService.registerSetUser(
                    "manager1",         // username
                    "password123",    // password (this will be hashed)
                    "ROLE_MANAGER",     // role
                    "Man",          // first name
                    "Ager",          // last name
                    "123 Manager Street", // address
                    "1980-01-01",     // dob
                    "manager1@example.com" // email
            );
        } catch (Exception e) {
            // User may already exist; ignore if so.
        }

        try {
            userService.registerSetUser(
                    "admin1",         // username
                    "password123",    // password (this will be hashed)
                    "ROLE_ADMIN",     // role
                    "Alice",          // first name
                    "Admin",          // last name
                    "123 Admin Street", // address
                    "1980-01-01",     // dob
                    "admin1@example.com" // email
            );
        } catch (Exception e) {
            // User may already exist; ignore if so.
        }
        // Create first admin account
        try {
            userService.registerSetUser(
                "admin1",         // username
                "password123",    // password (this will be hashed)
                "ROLE_ADMIN",     // role
                "Alice",          // first name
                "Admin",          // last name
                "123 Admin Street", // address
                "1980-01-01",     // dob
                "admin1@example.com" // email
            );
        } catch (Exception e) {
            // User may already exist; ignore if so.
        }

        // Create second admin account
        try {
            userService.registerSetUser(
                "admin2",
                "password123",
                "ROLE_ADMIN",
                "Bob",
                "Admin",
                "456 Admin Avenue",
                "1985-02-02",
                "admin2@example.com"
            );
        } catch (Exception e) {
            // Ignore duplicate user exception
        }

        // Create third admin account
        try {
            userService.registerSetUser(
                "admin3",
                "password123",
                "ROLE_ADMIN",
                "Carol",
                "Admin",
                "789 Admin Blvd",
                "1990-03-03",
                "admin3@example.com"
            );
        } catch (Exception e) {
            // Ignore duplicate user exception
        }
    }
}
