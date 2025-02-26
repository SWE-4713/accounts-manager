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

    private String firstName; // e.g., ROLE_USER, ROLE_ADMIN

    private String lastName; // e.g., ROLE_USER, ROLE_ADMIN

    private String address; // e.g., ROLE_USER, ROLE_ADMIN

    private String dob; // e.g., ROLE_USER, ROLE_ADMIN

    private String email; // e.g., ROLE_USER, ROLE_ADMIN
}
