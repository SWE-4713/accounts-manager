package com.example.FinanceProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate entryDate;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account; 
    // The account from the chart of accounts

    private BigDecimal debit;
    private BigDecimal credit;

    @Enumerated(EnumType.STRING)
    private JournalStatus status; // PENDING, APPROVED, REJECTED

    @Column(length = 500)
    private String comment; // manager's comment if rejected

    // Optionally: description or reference fields, e.g. "Service Revenue"
    private String description;

    // CreatedBy, ApprovedBy, etc. can be added if needed
    private String postReference; // Unique code for linking back to the journal entry
}
