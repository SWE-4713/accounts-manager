package com.example.FinanceProject.repository;

import com.example.FinanceProject.entity.JournalEntryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryAttachmentRepo extends JpaRepository<JournalEntryAttachment, Long> {
    // Additional custom queries (if needed) can be added here
}
