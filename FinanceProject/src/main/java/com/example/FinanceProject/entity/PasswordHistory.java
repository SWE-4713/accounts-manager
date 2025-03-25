package com.example.FinanceProject.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
   @Table(name = "password_history")
   @Data
   @NoArgsConstructor
   public class PasswordHistory {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       
       @ManyToOne(fetch = FetchType.LAZY)
       @JoinColumn(name = "user_id", nullable = false)
       private User user;
       
       @Column(nullable = false)
       private String passwordHash;
       
       @Column(nullable = false)
       private LocalDateTime createdAt;
       
       public PasswordHistory(User user, String passwordHash) {
           this.user = user;
           this.passwordHash = passwordHash;
           this.createdAt = LocalDateTime.now();
       }
   }