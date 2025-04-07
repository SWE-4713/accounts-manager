package com.example.FinanceProject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "journal_entry_lines")
public class JournalEntryLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The account affected by this line
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    // Transient property to bind account id from the form
    @Transient
    private Long accountId;

    // Debit amount (if applicable)
    @Column(precision = 15, scale = 2)
    private BigDecimal debit;

    // Credit amount (if applicable)
    @Column(precision = 15, scale = 2)
    private BigDecimal credit;

    // Link back to the parent journal entry
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;
}