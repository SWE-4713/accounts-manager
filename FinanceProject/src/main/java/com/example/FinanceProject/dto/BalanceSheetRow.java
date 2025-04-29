package com.example.FinanceProject.dto;

 import java.math.BigDecimal;

 import lombok.Data;
 import lombok.NoArgsConstructor; // Keep this for the default constructor

@Data
@NoArgsConstructor // Lombok generates the no-arg constructor automatically
public class BalanceSheetRow {
    private Long accountId; // Already present
    private String label;
    private BigDecimal amount; // Value to display
    private int displayColumn; // 1 for inner column, 2 for outer/total column, 0 for headers/blank
    private String styleClass; // CSS classes for styling (e.g., "header", "indent-1", "underline")
    private boolean showDollarSign; // Flag for controlling '$'

    // Constructor including accountId (for account rows)
    public BalanceSheetRow(Long accountId, String label, BigDecimal amount, int displayColumn, String styleClass, boolean showDollarSign) {
        this.accountId = accountId;
        this.label = label;
        this.amount = amount;
        this.displayColumn = displayColumn;
        this.styleClass = styleClass;
        this.showDollarSign = showDollarSign;
    }

    // Constructor for simple headers/totals (without accountId)
    public BalanceSheetRow(String label, BigDecimal amount, int displayColumn, String styleClass, boolean showDollarSign) {
        this(null, label, amount, displayColumn, styleClass, showDollarSign); // Pass null for accountId
    }


    // Constructor for simple headers/blank rows without amount/dollar sign/accountId
    public BalanceSheetRow(String label, String styleClass) {
        this(null, label, null, 0, styleClass, false); // Pass null for accountId
    }

    // REMOVED the duplicate no-argument constructor previously added here.
    // The @NoArgsConstructor annotation handles this.

}