package com.example.FinanceProject;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pending_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingUser {

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

    // Optional status field – here always "PENDING"
    private String status;
}
