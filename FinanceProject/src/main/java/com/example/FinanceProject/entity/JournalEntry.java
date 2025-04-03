package com.example.FinanceProject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "journal_entry")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Date of the entry
    private LocalDateTime entryDate = LocalDateTime.now();

    // Overall description (often empty)
    private String description;

    // Status: PENDING, APPROVED, or REJECTED
    private String status = "PENDING";

    // For manager rejection reason (if any)
    private String managerComment;

    private String attachmentPath;

    // List of transaction lines
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines;

    // Reference code to link back to account ledger
    private String postReference;
}

