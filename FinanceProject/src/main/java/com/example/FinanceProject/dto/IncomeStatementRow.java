package com.example.FinanceProject.dto;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IncomeStatementRow {
    private Long accountId; // Added field
    private String label;
    private BigDecimal amount;
    private boolean showDollarSign = false; // Flag for $ sign

    // Constructor for account items
    public IncomeStatementRow(Long accountId, String label, BigDecimal amount) {
        this.accountId = accountId; // Initialize accountId
        this.label = label;
        this.amount = amount;
    }

     // Constructor for headers/totals/blank rows (no accountId)
    public IncomeStatementRow(String label, BigDecimal amount) {
        this(null, label, amount); // Call the main constructor with null accountId
    }

     // Constructor just for label (e.g., headers)
    public IncomeStatementRow(String label) {
        this(null, label, null); // Call the main constructor with null accountId and amount
    }
}