// src/main/java/com/example/FinanceProject/controller/DashboardController.java
package com.example.FinanceProject.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.FinanceProject.dto.RatioInfo;
import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalEntryLine;
import com.example.FinanceProject.entity.JournalStatus;
import com.example.FinanceProject.service.AccountService;
import com.example.FinanceProject.service.JournalEntryService;


@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private JournalEntryService journalEntryService;
    @Autowired
    private AccountService accountService;


    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }

        // --- Fetch Actual Account Balances ---
        List<Account> allActiveAccounts;
        try {
             allActiveAccounts = accountService.getAllAccounts(Sort.unsorted())
                .stream()
                .filter(Account::isActive)
                .collect(Collectors.toList());
        } catch (Exception e) {
             log.error("Error fetching accounts for dashboard: {}", e.getMessage(), e);
             allActiveAccounts = Collections.emptyList();
             model.addAttribute("importantMessage", "Error fetching account data for ratio calculation.");
             model.addAttribute("messageType", "danger");
        }


        // --- Calculate Actual Financial Totals ---
        BigDecimal currentAssets = calculateSumByCategoryAndSubcategory(allActiveAccounts, "Asset", "Current Assets");
        BigDecimal totalAssets = calculateSumByCategory(allActiveAccounts, "Asset");
        BigDecimal currentLiabilities = calculateSumByCategoryAndSubcategory(allActiveAccounts, "Liability", "Current Liability");
        BigDecimal totalEquity = calculateSumByCategory(allActiveAccounts, "Equity");
        BigDecimal inventory = calculateInventoryBalance(allActiveAccounts);

        log.info("Calculated Dashboard Totals: TA={}, TE={}, CA={}, CL={}, Inv={}", totalAssets, totalEquity, currentAssets, currentLiabilities, inventory);


        // --- Fetch Real Monthly Data ---
        Map<String, Map<String, BigDecimal>> monthlyData = calculateMonthlyIncomeExpense();
        model.addAttribute("monthlyData", monthlyData);

        // --- Calculate Totals from Real Monthly Data ---
        BigDecimal totalIncomeFromMonths = monthlyData.values().stream()
                .map(m -> m.getOrDefault("income", BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpensesFromMonths = monthlyData.values().stream()
                .map(m -> m.getOrDefault("expense", BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netIncomeTotal = totalIncomeFromMonths.subtract(totalExpensesFromMonths);


        // --- Use Calculated Totals for Ratio Inputs ---
        BigDecimal salesRevenue = totalIncomeFromMonths;


        // Calculate Ratios (Formatted for display cards) using ACTUAL values
        List<RatioInfo> ratioInfos = calculateRatios(currentAssets, currentLiabilities, netIncomeTotal, totalAssets, totalEquity, salesRevenue, inventory);
        model.addAttribute("ratios", ratioInfos);

        // Pass raw ratio values for potential charting using ACTUAL values
        Map<String, BigDecimal> rawRatios = getRawRatios(currentAssets, currentLiabilities, netIncomeTotal, totalAssets, totalEquity, salesRevenue, inventory);
        model.addAttribute("rawRatios", rawRatios);


        // --- Message Section (Keep as is) ---
        try {
            List<JournalEntry> pendingEntries = journalEntryService.findByStatus(JournalStatus.PENDING.name());
            int pendingCount = pendingEntries != null ? pendingEntries.size() : 0;
            if (pendingCount > 0 && model.getAttribute("importantMessage") == null) {
                model.addAttribute("importantMessage", "You have " + pendingCount + " journal entries pending approval.");
                model.addAttribute("messageType", "warning");
            }
        } catch (Exception e) {
            log.error("Error fetching pending journal entries: {}", e.getMessage(), e);
            if (model.getAttribute("importantMessage") == null) {
                 model.addAttribute("importantMessage", "Could not fetch journal entry status.");
                 model.addAttribute("messageType", "danger");
            }
        }

        return "dashboard";
    }

    // --- Helper Methods to Calculate Sums from Account List ---
    // (Unchanged)
    private BigDecimal calculateSumByCategory(List<Account> accounts, String category) {
        if (accounts == null) return BigDecimal.ZERO;
        return accounts.stream()
            .filter(a -> category.equalsIgnoreCase(a.getAccountCategory()))
            .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSumByCategoryAndSubcategory(List<Account> accounts, String category, String subcategory) {
        if (accounts == null) return BigDecimal.ZERO;
        return accounts.stream()
             .filter(a -> category.equalsIgnoreCase(a.getAccountCategory()) && subcategory.equalsIgnoreCase(a.getAccountSubcategory()))
             .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
             .reduce(BigDecimal.ZERO, BigDecimal::add);
     }

    private BigDecimal calculateInventoryBalance(List<Account> accounts) {
        if (accounts == null) return BigDecimal.ZERO;
        return accounts.stream()
            .filter(a -> "Asset".equalsIgnoreCase(a.getAccountCategory()) &&
                         "Current Assets".equalsIgnoreCase(a.getAccountSubcategory()) &&
                         a.getAccountName() != null &&
                         a.getAccountName().equalsIgnoreCase("Inventory"))
            .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    // --- calculateMonthlyIncomeExpense method (remains unchanged) ---
    private Map<String, Map<String, BigDecimal>> calculateMonthlyIncomeExpense() {
        // ... (Implementation from previous step remains the same) ...
         Map<String, Map<String, BigDecimal>> monthlyTotals = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        Month currentMonthEnum = today.getMonth();

        for (int i = 1; i <= currentMonthEnum.getValue(); i++) {
            Month month = Month.of(i);
            Map<String, BigDecimal> monthMap = new LinkedHashMap<>();
            monthMap.put("income", BigDecimal.ZERO);
            monthMap.put("expense", BigDecimal.ZERO);
            monthMap.put("netProfit", BigDecimal.ZERO);
            String monthKey = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            monthlyTotals.put(monthKey, monthMap);
        }

        LocalDate startOfYear = LocalDate.of(currentYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);

        List<JournalEntry> approvedEntries = null;
        try {
            approvedEntries = journalEntryService.findByStatus(JournalStatus.APPROVED.name())
                .stream()
                .filter(entry -> entry.getEntryDate() != null && !entry.getEntryDate().isBefore(startOfYear) && !entry.getEntryDate().isAfter(endOfYear))
                .collect(Collectors.toList());
        } catch(Exception e) {
             log.error("Error fetching approved journal entries: {}", e.getMessage(), e);
             approvedEntries = new ArrayList<>();
        }


        for (JournalEntry entry : approvedEntries) {
            Month month = entry.getEntryDate().getMonth();
            if (month.getValue() <= currentMonthEnum.getValue()) {
                String monthKey = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                Map<String, BigDecimal> currentMonthTotals = monthlyTotals.get(monthKey);
                if (currentMonthTotals == null) continue;

                if (entry.getLines() != null) {
                    for (JournalEntryLine line : entry.getLines()) {
                        if (line == null) continue;
                        Account account = line.getAccount();
                        if (account == null || account.getAccountCategory() == null) continue;

                        BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
                        BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;

                        if ("Revenue".equalsIgnoreCase(account.getAccountCategory())) {
                            BigDecimal revenueChange = credit.subtract(debit);
                            currentMonthTotals.put("income", currentMonthTotals.get("income").add(revenueChange));
                        } else if ("Expense".equalsIgnoreCase(account.getAccountCategory())) {
                            BigDecimal expenseChange = debit.subtract(credit);
                            currentMonthTotals.put("expense", currentMonthTotals.get("expense").add(expenseChange));
                        }
                    }
                }
            }
        }

        for (Map<String, BigDecimal> monthMap : monthlyTotals.values()) {
            BigDecimal income = monthMap.get("income");
            BigDecimal expense = monthMap.get("expense");
            monthMap.put("netProfit", income.subtract(expense));
        }
        return monthlyTotals;
    }


    // --- calculateRatios method (Now receives actual calculated values) ---
    // (Unchanged)
    private List<RatioInfo> calculateRatios(BigDecimal currentAssets, BigDecimal currentLiabilities, BigDecimal netIncome, BigDecimal totalAssets, BigDecimal totalEquity, BigDecimal salesRevenue, BigDecimal inventory) {
        // ... (Implementation remains the same) ...
         List<RatioInfo> ratioInfos = new ArrayList<>();
        DecimalFormat pctFormat = new DecimalFormat("0.0%");
        DecimalFormat ratioFormat = new DecimalFormat("0.00");

        // 1. Current Ratio
        if (currentLiabilities != null && currentLiabilities.compareTo(BigDecimal.ZERO) != 0 && currentAssets != null) {
            BigDecimal currentRatio = currentAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP);
            String color = getRatioColorClass("Current Ratio", currentRatio);
            ratioInfos.add(new RatioInfo("Current Ratio", ratioFormat.format(currentRatio), color, "Measures short-term liquidity"));
        } else {
             ratioInfos.add(new RatioInfo("Current Ratio", "N/A", "grey", "Measures short-term liquidity"));
        }

        // 2. Return on Assets (ROA)
        if (totalAssets != null && totalAssets.compareTo(BigDecimal.ZERO) != 0 && netIncome != null) {
             BigDecimal roa = netIncome.divide(totalAssets, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
             String color = getRatioColorClass("ROA", roa);
             ratioInfos.add(new RatioInfo("Return on Assets (ROA)", pctFormat.format(roa.divide(BigDecimal.valueOf(100))), color, "Profit per dollar of assets"));
        } else {
             ratioInfos.add(new RatioInfo("Return on Assets (ROA)", "N/A", "grey", "Profit per dollar of assets"));
        }

        // 3. Return on Equity (ROE)
        if (totalEquity != null && totalEquity.compareTo(BigDecimal.ZERO) != 0 && netIncome != null) {
             BigDecimal roe = netIncome.divide(totalEquity, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
             String color = getRatioColorClass("ROE", roe);
             ratioInfos.add(new RatioInfo("Return on Equity (ROE)", pctFormat.format(roe.divide(BigDecimal.valueOf(100))), color, "Profit per dollar of equity"));
        } else {
             ratioInfos.add(new RatioInfo("Return on Equity (ROE)", "N/A", "grey", "Profit per dollar of equity"));
        }

         // 4. Net Profit Margin
         if (salesRevenue != null && salesRevenue.compareTo(BigDecimal.ZERO) != 0 && netIncome != null) {
             BigDecimal npm = netIncome.divide(salesRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
             String color = getRatioColorClass("Net Profit Margin", npm);
             ratioInfos.add(new RatioInfo("Net Profit Margin", pctFormat.format(npm.divide(BigDecimal.valueOf(100))), color, "Profit per dollar of sales"));
         } else {
             ratioInfos.add(new RatioInfo("Net Profit Margin", "N/A", "grey", "Profit per dollar of sales"));
         }

        // 5. Asset Turnover
        if (totalAssets != null && totalAssets.compareTo(BigDecimal.ZERO) != 0 && salesRevenue != null) {
             BigDecimal assetTurnover = salesRevenue.divide(totalAssets, 4, RoundingMode.HALF_UP);
             String color = getRatioColorClass("Asset Turnover", assetTurnover);
             ratioInfos.add(new RatioInfo("Asset Turnover", ratioFormat.format(assetTurnover), color, "Sales generated per dollar of assets"));
        } else {
             ratioInfos.add(new RatioInfo("Asset Turnover", "N/A", "grey", "Sales generated per dollar of assets"));
        }

         // 6. Quick Ratio
         if (currentLiabilities != null && currentLiabilities.compareTo(BigDecimal.ZERO) != 0 && currentAssets != null && inventory != null) {
             BigDecimal quickAssets = currentAssets.subtract(inventory);
             if (currentLiabilities.compareTo(BigDecimal.ZERO) != 0) {
                 BigDecimal quickRatio = quickAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP);
                 String color = getRatioColorClass("Quick Ratio", quickRatio);
                 ratioInfos.add(new RatioInfo("Quick Ratio (Acid Test)", ratioFormat.format(quickRatio), color, "Liquidity excluding inventory"));
             } else {
                 ratioInfos.add(new RatioInfo("Quick Ratio (Acid Test)", "N/A", "grey", "Liquidity excluding inventory"));
             }
         } else {
              ratioInfos.add(new RatioInfo("Quick Ratio (Acid Test)", "N/A", "grey", "Liquidity excluding inventory"));
         }
        return ratioInfos;
    }


    // --- getRawRatios method (Now receives actual calculated values) ---
    // (Unchanged)
    private Map<String, BigDecimal> getRawRatios(BigDecimal currentAssets, BigDecimal currentLiabilities, BigDecimal netIncome, BigDecimal totalAssets, BigDecimal totalEquity, BigDecimal salesRevenue, BigDecimal inventory) {
        // ... (Implementation remains the same) ...
        Map<String, BigDecimal> rawRatios = new LinkedHashMap<>();
        BigDecimal zero = BigDecimal.ZERO;

        // Current Ratio
        if (currentLiabilities != null && currentLiabilities.compareTo(zero) != 0 && currentAssets != null) {
            try {
                rawRatios.put("Current Ratio", currentAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP));
            } catch (ArithmeticException e) { log.error("Division by zero avoided calculating Current Ratio. CL={}", currentLiabilities); rawRatios.put("Current Ratio", zero); }
        } else { rawRatios.put("Current Ratio", zero); }

        // ROA (%)
        if (totalAssets != null && totalAssets.compareTo(zero) != 0 && netIncome != null) {
             try { rawRatios.put("ROA", netIncome.divide(totalAssets, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
             } catch (ArithmeticException e) { log.error("Division by zero avoided calculating ROA. TA={}", totalAssets); rawRatios.put("ROA", zero); }
        } else { rawRatios.put("ROA", zero); }

        // ROE (%)
        if (totalEquity != null && totalEquity.compareTo(zero) != 0 && netIncome != null) {
             try { rawRatios.put("ROE", netIncome.divide(totalEquity, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
             } catch (ArithmeticException e) { log.error("Division by zero avoided calculating ROE. TE={}", totalEquity); rawRatios.put("ROE", zero); }
        } else { rawRatios.put("ROE", zero); }

        // Net Profit Margin (%)
        if (salesRevenue != null && salesRevenue.compareTo(zero) != 0 && netIncome != null) {
            try { rawRatios.put("Net Profit Margin", netIncome.divide(salesRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
            } catch (ArithmeticException e) { log.error("Division by zero avoided calculating Net Profit Margin. Sales={}", salesRevenue); rawRatios.put("Net Profit Margin", zero); }
        } else { rawRatios.put("Net Profit Margin", zero); }

        // Asset Turnover (Ratio)
        if (totalAssets != null && totalAssets.compareTo(zero) != 0 && salesRevenue != null) {
             try { rawRatios.put("Asset Turnover", salesRevenue.divide(totalAssets, 4, RoundingMode.HALF_UP));
             } catch (ArithmeticException e) { log.error("Division by zero avoided calculating Asset Turnover. TA={}", totalAssets); rawRatios.put("Asset Turnover", zero); }
        } else { rawRatios.put("Asset Turnover", zero); }

        // Quick Ratio (Ratio)
        if (currentLiabilities != null && currentLiabilities.compareTo(zero) != 0 && currentAssets != null && inventory != null) {
            BigDecimal quickAssets = currentAssets.subtract(inventory);
             try { rawRatios.put("Quick Ratio", quickAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP));
             } catch (ArithmeticException e) { log.error("Division by zero avoided calculating Quick Ratio. CL={}", currentLiabilities); rawRatios.put("Quick Ratio", zero); }
        } else { rawRatios.put("Quick Ratio", zero); }

        return rawRatios;
    }


    // --- CORRECTED getRatioColorClass helper method ---
     private String getRatioColorClass(String ratioName, BigDecimal value) {
         if (value == null) return "grey";
         switch (ratioName) {
             case "Current Ratio":
                 return value.compareTo(new BigDecimal("2.0")) >= 0 ? "green" : (value.compareTo(new BigDecimal("1.0")) >= 0 ? "yellow" : "red");
             case "Quick Ratio":
                 return value.compareTo(new BigDecimal("1.0")) >= 0 ? "green" : (value.compareTo(new BigDecimal("0.5")) >= 0 ? "yellow" : "red");
             case "Net Profit Margin": case "ROA": case "ROE":
                 // Corrected lines: wrap literals with BigDecimal.valueOf()
                 BigDecimal targetHigh = ratioName.equals("Net Profit Margin") ? BigDecimal.valueOf(20) : (ratioName.equals("ROE") ? BigDecimal.valueOf(15) : BigDecimal.valueOf(5));
                 BigDecimal targetMid = ratioName.equals("Net Profit Margin") ? BigDecimal.valueOf(10) : (ratioName.equals("ROE") ? BigDecimal.valueOf(10) : BigDecimal.valueOf(2.5));
                 return value.compareTo(targetHigh) >= 0 ? "green" : (value.compareTo(targetMid) >= 0 ? "yellow" : "red");
            case "Asset Turnover":
                return value.compareTo(new BigDecimal("1.0")) >= 0 ? "green" : (value.compareTo(new BigDecimal("0.5")) >= 0 ? "yellow" : "red");
             default:
                 return "grey";
         }
     }


    // --- Original Placeholder Method (Kept but commented out in showDashboard) ---
    private Map<String, Map<String, BigDecimal>> getPlaceholderMonthlyData() {
        // ... (Implementation remains unchanged) ...
         Map<String, Map<String, BigDecimal>> data = new LinkedHashMap<>();
        List<String> months = Arrays.asList(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        );
        BigDecimal lastIncome = new BigDecimal("20000");
        BigDecimal lastExpense = new BigDecimal("15000");
        for (String month : months) {
             Map<String, BigDecimal> values = new LinkedHashMap<>();
             BigDecimal income = lastIncome.multiply(BigDecimal.valueOf(1 + (Math.random() - 0.4) * 0.3)).setScale(2, RoundingMode.HALF_UP);
             BigDecimal expense = lastExpense.multiply(BigDecimal.valueOf(1 + (Math.random() - 0.4) * 0.2)).setScale(2, RoundingMode.HALF_UP);
             BigDecimal netProfit = income.subtract(expense);
             values.put("income", income); values.put("expense", expense); values.put("netProfit", netProfit);
             data.put(month, values);
             lastIncome = income.compareTo(BigDecimal.ZERO) > 0 ? income : new BigDecimal("20000");
             lastExpense = expense.compareTo(BigDecimal.ZERO) > 0 ? expense : new BigDecimal("15000");
        }
        return data;
    }

}