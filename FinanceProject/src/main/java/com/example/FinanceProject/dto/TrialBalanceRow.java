package com.example.FinanceProject.dto;

import java.math.BigDecimal;

import com.example.FinanceProject.entity.Account;

import lombok.AllArgsConstructor;
import lombok.Data;          // Lombok generates getters/setters
import lombok.NoArgsConstructor;

@Data      
@NoArgsConstructor         // keeps the empty ctor
@AllArgsConstructor                  // generates getDebit() & getCredit()
public class TrialBalanceRow {

    private String  accountNumber;
    private String  accountName;
    private BigDecimal debit  = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;

    public TrialBalanceRow(Account account,
                        BigDecimal debit,
                        BigDecimal credit) {
        this.accountNumber = account.getAccountNumber();
        this.accountName   = account.getAccountName();
        this.debit         = debit;
        this.credit        = credit;
    }
}