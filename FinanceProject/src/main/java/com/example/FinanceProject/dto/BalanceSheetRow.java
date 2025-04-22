package com.example.FinanceProject.dto;

 import lombok.AllArgsConstructor;
 import lombok.Data;
 import lombok.NoArgsConstructor;
 import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BalanceSheetRow {
    private String label;
    private BigDecimal amount; // Value to display
    private int displayColumn; // 1 for inner column, 2 for outer/total column, 0 for headers/blank
    private String styleClass; // CSS classes for styling (e.g., "header", "indent-1", "underline")
    private boolean showDollarSign; // New flag for controlling '$'

    // Constructor including the new flag
    public BalanceSheetRow(String label, BigDecimal amount, int displayColumn, String styleClass, boolean showDollarSign) {
        this.label = label;
        this.amount = amount;
        this.displayColumn = displayColumn;
        this.styleClass = styleClass;
        this.showDollarSign = showDollarSign;
    }

    // Constructor for simple headers/blank rows without amount/dollar sign
    public BalanceSheetRow(String label, String styleClass) {
        this(label, null, 0, styleClass, false);
    }
}