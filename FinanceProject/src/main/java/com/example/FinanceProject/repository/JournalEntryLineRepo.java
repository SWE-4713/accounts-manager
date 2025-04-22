/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

 package com.example.FinanceProject.repository;

 import java.util.List;               // ← add this

 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
 import org.springframework.data.repository.query.Param;

 import com.example.FinanceProject.entity.JournalEntryLine;
 import com.example.FinanceProject.entity.JournalStatus;
/**
 *
 * @author huyng
 */
public interface JournalEntryLineRepo extends JpaRepository<JournalEntryLine, Long> {
    @Query("""
      SELECT line
        FROM JournalEntryLine line
        JOIN line.journalEntry je
       WHERE line.account.id = :accountId
         AND je.status        = :status
       ORDER BY je.entryDate, line.id
    """)
    List<JournalEntryLine> findByAccountIdAndStatus(
        @Param("accountId") Long accountId,
        @Param("status")    JournalStatus status);
  }
