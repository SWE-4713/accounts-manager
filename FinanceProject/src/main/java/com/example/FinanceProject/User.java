package com.example.FinanceProject;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

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

    @Column(name = "failed_attempt", columnDefinition = "integer default 0")
    private int failedAttempt;

    @Column(name = "account_non_locked", columnDefinition = "boolean default true")
    private boolean accountNonLocked;

    @Column(name = "lock_time")
    private Date lockTime;

    @Column(name = "password_update_date")
    private Date passwordUpdateDate;


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

    @Setter
    @Getter
    private boolean passwordExpiryNotificationSent = false;

}