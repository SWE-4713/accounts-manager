package com.example.FinanceProject.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class RetainedEarningsReport {
   private BigDecimal beginningBalance;
    private BigDecimal netIncome;
    private BigDecimal dividends;
    private BigDecimal endingBalance;
    private LocalDate startDate; // Ensure this field exists
    private LocalDate endDate;   // Ensure this field exists
}