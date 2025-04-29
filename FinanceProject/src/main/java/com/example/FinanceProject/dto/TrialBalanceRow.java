package com.example.FinanceProject.dto;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
// Removed @AllArgsConstructor to add custom constructor
public class TrialBalanceRow {
    private Long accountId;
    private String  accountNumber;
    private String  accountName;
    private BigDecimal debit  = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;
    private boolean showDebitDollarSign = false;  // Flag for $ on debit
    private boolean showCreditDollarSign = false; // Flag for $ on credit

    // *** UPDATED Constructor to include accountId ***
    public TrialBalanceRow(Long accountId, String accountNumber, String accountName, BigDecimal debit, BigDecimal credit) {
        this.accountId     = accountId; // Initialize accountId
        this.accountNumber = accountNumber;
        this.accountName   = accountName;
        this.debit         = debit != null ? debit : BigDecimal.ZERO;
        this.credit        = credit != null ? credit : BigDecimal.ZERO;
        // Flags default to false
    }
}