// EventLog.java
package com.example.FinanceProject;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "event_logs")
@Data
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Unique auto-generated ID (req. 15)
    
    private Long userId; // ID of the user who made the change
    
    private LocalDateTime timestamp;
    
    // Action type: e.g., ADD, MODIFY, DEACTIVATE
    private String action;
    
    // Save before and after images as large objects (you can store JSON strings)
    @Lob
    private String beforeImage;
    
    @Lob
    private String afterImage;
}
