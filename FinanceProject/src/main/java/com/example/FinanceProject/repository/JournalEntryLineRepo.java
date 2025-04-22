/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

 package com.example.FinanceProject.repository;

 import java.time.LocalDate;
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
    
    // Query for calculating balance between two dates (used for Income Statement/RE changes)
    @Query("""
      SELECT l
        FROM JournalEntryLine l
        JOIN l.journalEntry je
       WHERE l.account.id = :acctId
         AND je.status     = com.example.FinanceProject.entity.JournalStatus.APPROVED
         AND je.entryDate BETWEEN :start AND :end
    """)
    List<JournalEntryLine> findLinesForPeriod( // Renamed for clarity
            @Param("acctId") Long accountId,
            @Param("start")  LocalDate start,
            @Param("end")    LocalDate end);
    
    @Query("""
      SELECT l
        FROM JournalEntryLine l
        JOIN l.journalEntry je
        WHERE l.account.id = :acctId
        AND je.status     = com.example.FinanceProject.entity.JournalStatus.APPROVED
        AND je.entryDate <= :asOfDate
    """)
    List<JournalEntryLine> findLinesForBalanceAsOf(
            @Param("acctId") Long accountId,
            @Param("asOfDate") LocalDate asOfDate);
}
