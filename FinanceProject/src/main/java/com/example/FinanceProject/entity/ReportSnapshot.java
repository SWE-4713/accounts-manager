/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.example.FinanceProject.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="report_snapshots")
public class ReportSnapshot {
  @Id 
  @GeneratedValue
  private Long id;

  private String  reportType;   // "TB", "IS", "BS", "RE"
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDateTime generatedAt = LocalDateTime.now();

  @Lob
  private String  payloadJson;   // raw JSON of rows or DTO
  // + getters/setters
}