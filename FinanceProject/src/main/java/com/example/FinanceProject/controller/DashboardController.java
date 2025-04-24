// src/main/java/com/example/FinanceProject/controller/DashboardController.java
package com.example.FinanceProject.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.FinanceProject.dto.RatioInfo; // To preserve month order
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.service.JournalEntryService;

@Controller
public class DashboardController {

    // Inject necessary services
    // @Autowired
    // private FinancialReportService financialReportService;
    @Autowired
    private JournalEntryService journalEntryService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }

        // --- Placeholder Financial Data ---
        // Replace with actual service calls
        BigDecimal currentAssets = new BigDecimal("50000");
        BigDecimal currentLiabilities = new BigDecimal("20000");
        BigDecimal netIncomeTotal = new BigDecimal("45000"); // Renamed to avoid clash
        BigDecimal totalAssets = new BigDecimal("250000");
        BigDecimal totalEquity = new BigDecimal("150000");
        BigDecimal salesRevenue = new BigDecimal("300000");
        BigDecimal inventory = new BigDecimal("15000");

        // Calculate Ratios (Formatted for display cards)
        List<RatioInfo> ratioInfos = calculateRatios(currentAssets, currentLiabilities, netIncomeTotal, totalAssets, totalEquity, salesRevenue, inventory);
        model.addAttribute("ratios", ratioInfos);

        // --- Data for Charts ---
        // Pass raw ratio values for potential charting (e.g., gauges)
        Map<String, BigDecimal> rawRatios = getRawRatios(currentAssets, currentLiabilities, netIncomeTotal, totalAssets, totalEquity, salesRevenue, inventory);
        model.addAttribute("rawRatios", rawRatios);

        // Placeholder Monthly Income/Expense Data for Bar Chart
        // In a real app, fetch this from DB (e.g., sum journal entries by month)
        Map<String, Map<String, BigDecimal>> monthlyData = getPlaceholderMonthlyData();
        model.addAttribute("monthlyData", monthlyData);

        // Placeholder Budget Data for Doughnut Charts
        BigDecimal incomeBudget = new BigDecimal("500000");
        BigDecimal expenseBudget = new BigDecimal("350000");
        BigDecimal actualIncome = salesRevenue; // Using sales revenue as actual income placeholder
        BigDecimal actualExpenses = salesRevenue.subtract(netIncomeTotal); // Derived placeholder

        BigDecimal incomeAchievedPercent = BigDecimal.ZERO;
        if (incomeBudget.compareTo(BigDecimal.ZERO) != 0) {
            incomeAchievedPercent = actualIncome.divide(incomeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        BigDecimal expenseUsedPercent = BigDecimal.ZERO;
        if (expenseBudget.compareTo(BigDecimal.ZERO) != 0) {
            expenseUsedPercent = actualExpenses.divide(expenseBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        model.addAttribute("incomeBudget", incomeBudget);
        model.addAttribute("expenseBudget", expenseBudget);
        model.addAttribute("incomeAchievedPercent", incomeAchievedPercent.min(BigDecimal.valueOf(100))); // Cap at 100% for chart
        model.addAttribute("expenseUsedPercent", expenseUsedPercent.min(BigDecimal.valueOf(100))); // Cap at 100%

        // --- Message Section ---
        try {
            List<JournalEntry> pendingEntries = journalEntryService.findByStatus("PENDING");
            int pendingCount = pendingEntries != null ? pendingEntries.size() : 0;
            if (pendingCount > 0) {
                model.addAttribute("importantMessage", "You have " + pendingCount + " journal entries pending approval.");
                model.addAttribute("messageType", "warning");
            }
        } catch (Exception e) {
            model.addAttribute("importantMessage", "Could not fetch journal entry status.");
            model.addAttribute("messageType", "danger");
        }

        return "dashboard";
    }

    // --- Keep the calculateRatios method for RatioInfo DTOs (for cards) ---
    private List<RatioInfo> calculateRatios(BigDecimal currentAssets, BigDecimal currentLiabilities, BigDecimal netIncome, BigDecimal totalAssets, BigDecimal totalEquity, BigDecimal salesRevenue, BigDecimal inventory) {
        // ... (previous implementation remains unchanged) ...
        List<RatioInfo> ratioInfos = new ArrayList<>();
        DecimalFormat pctFormat = new DecimalFormat("0.0%");
        DecimalFormat ratioFormat = new DecimalFormat("0.00");

        // 1. Current Ratio
        if (currentLiabilities != null && currentLiabilities.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal currentRatio = currentAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP);
            String color;
            if (currentRatio.compareTo(new BigDecimal("2.0")) > 0) {
                color = "green";
            } else if (currentRatio.compareTo(new BigDecimal("1.0")) >= 0) {
                color = "yellow";
            } else {
                color = "red";
            }
            ratioInfos.add(new RatioInfo("Current Ratio", ratioFormat.format(currentRatio), color, "Measures short-term liquidity"));
        } else {
             ratioInfos.add(new RatioInfo("Current Ratio", "N/A", "grey", "Measures short-term liquidity"));
        }

        // 2. Return on Assets (ROA)
        if (totalAssets != null && totalAssets.compareTo(BigDecimal.ZERO) != 0) {
             BigDecimal roa = netIncome.divide(totalAssets, 4, RoundingMode.HALF_UP);
             String color;
             if (roa.compareTo(new BigDecimal("0.20")) > 0) {
                 color = "green";
             } else if (roa.compareTo(new BigDecimal("0.05")) > 0) {
                 color = "yellow";
             } else {
                 color = "red";
             }
             ratioInfos.add(new RatioInfo("Return on Assets (ROA)", pctFormat.format(roa), color, "Profit per dollar of assets"));
        } else {
             ratioInfos.add(new RatioInfo("Return on Assets (ROA)", "N/A", "grey", "Profit per dollar of assets"));
        }

        // 3. Return on Equity (ROE)
        if (totalEquity != null && totalEquity.compareTo(BigDecimal.ZERO) != 0) {
             BigDecimal roe = netIncome.divide(totalEquity, 4, RoundingMode.HALF_UP);
             String color;
             if (roe.compareTo(new BigDecimal("0.20")) > 0) {
                 color = "green";
             } else if (roe.compareTo(new BigDecimal("0.05")) > 0) {
                 color = "yellow";
             } else {
                 color = "red";
             }
             ratioInfos.add(new RatioInfo("Return on Equity (ROE)", pctFormat.format(roe), color, "Profit per dollar of equity"));
        } else {
             ratioInfos.add(new RatioInfo("Return on Equity (ROE)", "N/A", "grey", "Profit per dollar of equity"));
        }

         // 4. Net Profit Margin
         if (salesRevenue != null && salesRevenue.compareTo(BigDecimal.ZERO) != 0) {
             BigDecimal npm = netIncome.divide(salesRevenue, 4, RoundingMode.HALF_UP);
             String color;
             if (npm.compareTo(new BigDecimal("0.20")) >= 0) {
                 color = "green";
             } else if (npm.compareTo(new BigDecimal("0.10")) >= 0) {
                 color = "yellow";
             } else {
                 color = "red";
             }
             ratioInfos.add(new RatioInfo("Net Profit Margin", pctFormat.format(npm), color, "Profit per dollar of sales"));
         } else {
             ratioInfos.add(new RatioInfo("Net Profit Margin", "N/A", "grey", "Profit per dollar of sales"));
         }

        // 5. Asset Turnover
        if (totalAssets != null && totalAssets.compareTo(BigDecimal.ZERO) != 0) {
             BigDecimal assetTurnover = salesRevenue.divide(totalAssets, 4, RoundingMode.HALF_UP);
             String color;
             if (assetTurnover.compareTo(new BigDecimal("0.25")) > 0) {
                 color = "green";
             } else if (assetTurnover.compareTo(BigDecimal.ZERO) >= 0) {
                 color = "yellow";
             } else {
                 color = "red";
             }
             ratioInfos.add(new RatioInfo("Asset Turnover", ratioFormat.format(assetTurnover), color, "Sales generated per dollar of assets"));
        } else {
             ratioInfos.add(new RatioInfo("Asset Turnover", "N/A", "grey", "Sales generated per dollar of assets"));
        }

         // 6. Quick Ratio
         if (currentLiabilities != null && currentLiabilities.compareTo(BigDecimal.ZERO) != 0) {
             BigDecimal quickAssets = currentAssets.subtract(inventory);
             BigDecimal quickRatio = quickAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP);
             String color;
             if (quickRatio.compareTo(new BigDecimal("1.0")) > 0) {
                 color = "green";
             } else if (quickRatio.compareTo(BigDecimal.ZERO) >= 0) {
                 color = "yellow";
             } else {
                 color = "red";
             }
             ratioInfos.add(new RatioInfo("Quick Ratio (Acid Test)", ratioFormat.format(quickRatio), color, "Liquidity excluding inventory"));
         } else {
              ratioInfos.add(new RatioInfo("Quick Ratio (Acid Test)", "N/A", "grey", "Liquidity excluding inventory"));
         }

        return ratioInfos;
    }

    // --- NEW: Calculate Raw Ratios for Charts ---
    private Map<String, BigDecimal> getRawRatios(BigDecimal currentAssets, BigDecimal currentLiabilities, BigDecimal netIncome, BigDecimal totalAssets, BigDecimal totalEquity, BigDecimal salesRevenue, BigDecimal inventory) {
        Map<String, BigDecimal> rawRatios = new LinkedHashMap<>();
        BigDecimal zero = BigDecimal.ZERO;

        // Current Ratio
        if (currentLiabilities != null && currentLiabilities.compareTo(zero) != 0) {
            rawRatios.put("Current Ratio", currentAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP));
        } else { rawRatios.put("Current Ratio", zero); }

        // ROA
        if (totalAssets != null && totalAssets.compareTo(zero) != 0) {
             rawRatios.put("ROA", netIncome.divide(totalAssets, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))); // As Percentage
        } else { rawRatios.put("ROA", zero); }

        // ROE
        if (totalEquity != null && totalEquity.compareTo(zero) != 0) {
             rawRatios.put("ROE", netIncome.divide(totalEquity, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))); // As Percentage
        } else { rawRatios.put("ROE", zero); }

        // Net Profit Margin
        if (salesRevenue != null && salesRevenue.compareTo(zero) != 0) {
             rawRatios.put("Net Profit Margin", netIncome.divide(salesRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))); // As Percentage
        } else { rawRatios.put("Net Profit Margin", zero); }

        // Asset Turnover
        if (totalAssets != null && totalAssets.compareTo(zero) != 0) {
             rawRatios.put("Asset Turnover", salesRevenue.divide(totalAssets, 4, RoundingMode.HALF_UP)); // As Ratio
        } else { rawRatios.put("Asset Turnover", zero); }

        // Quick Ratio
        if (currentLiabilities != null && currentLiabilities.compareTo(zero) != 0) {
            BigDecimal quickAssets = currentAssets.subtract(inventory);
            rawRatios.put("Quick Ratio", quickAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP));
        } else { rawRatios.put("Quick Ratio", zero); }

        return rawRatios;
    }

     // --- NEW: Placeholder for Monthly Income/Expense Data ---
    private Map<String, Map<String, BigDecimal>> getPlaceholderMonthlyData() {
        Map<String, Map<String, BigDecimal>> data = new LinkedHashMap<>(); // LinkedHashMap preserves insertion order (month order)
        List<String> months = Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

        // Example random-ish data generation
        BigDecimal lastIncome = new BigDecimal("20000");
        BigDecimal lastExpense = new BigDecimal("15000");

        for (String month : months) {
             Map<String, BigDecimal> values = new LinkedHashMap<>();
             // Simulate some variation
             BigDecimal income = lastIncome.multiply(BigDecimal.valueOf(1 + (Math.random() - 0.4) * 0.3)).setScale(2, RoundingMode.HALF_UP);
             BigDecimal expense = lastExpense.multiply(BigDecimal.valueOf(1 + (Math.random() - 0.4) * 0.2)).setScale(2, RoundingMode.HALF_UP);
             BigDecimal netProfit = income.subtract(expense);

             values.put("income", income);
             values.put("expense", expense);
             values.put("netProfit", netProfit);
             data.put(month, values);

             lastIncome = income;
             lastExpense = expense;
        }
        return data;
    }

}