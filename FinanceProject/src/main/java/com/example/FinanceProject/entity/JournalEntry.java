package com.example.FinanceProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name= "journal_entries")
public class JournalEntry {

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate entryDate;

    private BigDecimal debit;
    private BigDecimal credit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JournalStatus status; // PENDING, APPROVED, REJECTED

    @Column(length = 500)
    private String managerComment; // manager's comment if rejected

    // Optionally: description or reference fields, e.g. "Service Revenue"
    private String description;

    // List of transaction lines
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();

    // Reference code to link back to account ledger
    private String postReference;

    // New field to store the attachment path for source documents
    @Column(name = "attachment_path")
    private String attachmentPath;
}
