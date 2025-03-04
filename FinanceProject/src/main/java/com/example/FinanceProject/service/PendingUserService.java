package com.example.FinanceProject.service;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pending_users")
public class PendingUserService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role; 
    private String firstName;
    private String lastName;
    private String address;
    private String dob;
    private String email;

    // Status will be "PENDING", "ACCEPTED", or "DENIED"
    private String status;
}
