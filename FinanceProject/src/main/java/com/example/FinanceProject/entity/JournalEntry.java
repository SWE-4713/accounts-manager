package com.example.FinanceProject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name= "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Auto-incremented journal entry ID

    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JournalStatus status; // e.g., PENDING, APPROVED, REJECTED

    // Manager comment (for approvals/rejections) remains if needed.
    private String managerComment;

    // Header-level description and the optional comment entered at creation.
    private String description;
    private String entryComment;

    // Path to the file attachment (optional)
    @Column(name = "attachment_path")
    private String attachmentPath;

    // New field to store the username of the user that created this entry.
    private String createdBy;
    
    // One-to-Many relationship with lines (each representing a debit or a credit)
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();

    // Transient getters to calculate totals from the lines.
    @Transient
    public BigDecimal getTotalDebit() {
        return lines.stream()
                .map(line -> line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getTotalCredit() {
        return lines.stream()
                .map(line -> line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
