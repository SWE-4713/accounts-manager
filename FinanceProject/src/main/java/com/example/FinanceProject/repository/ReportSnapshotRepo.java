/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.example.FinanceProject.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.FinanceProject.entity.ReportSnapshot;

public interface  ReportSnapshotRepo extends JpaRepository<ReportSnapshot,Long> {
    ReportSnapshot findTopByReportTypeAndStartDateAndEndDateOrderByGeneratedAtDesc(
        String reportType,
        LocalDate startDate,
        LocalDate endDate
    );

    // NEW: retrieve all snapshots of a type, newest first
    List<ReportSnapshot> findByReportTypeOrderByGeneratedAtDesc(String reportType);
}
