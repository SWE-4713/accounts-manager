package com.example.FinanceProject.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor        // <-- Essential for Jackson deserialization
@AllArgsConstructor       // <-- Used by the service to create instances
public class IncomeStatementRow {
    private String label;
    private BigDecimal amount;
}