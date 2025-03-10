package com.example.FinanceProject;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role; // e.g., ROLE_USER, ROLE_ADMIN

    private String firstName;
    private String lastName;
    private String address;
    private String dob;
    private String email;
    
    // New field to store acceptance status ("ACCEPTED", "PENDING", etc.)
    private String status;
}