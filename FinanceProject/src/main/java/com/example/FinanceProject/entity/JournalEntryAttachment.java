package com.example.FinanceProject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "journal_entry_attachments")
public class JournalEntryAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Original file name (as uploaded)
    private String fileName;
    
    // File system path where the file is stored
    private String filePath;
    
    // Many attachments can belong to one journal entry
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;
}
