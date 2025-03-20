package com.example.FinanceProject;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_logs")
@Data
@NoArgsConstructor
public class LoginLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private LocalDateTime loginTime;
    
    @Column
    private String ipAddress;
    
    @Column
    private String userAgent;
    
    @Column
    private boolean successful;

    public LoginLog(User user, String username, String ipAddress, String userAgent, boolean successful) {
        this.user = user;
        this.username = username;
        this.loginTime = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.successful = successful;
    }
}