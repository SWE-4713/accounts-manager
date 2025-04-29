package com.example.FinanceProject.service;

 // --- Keep existing imports ---
 import com.example.FinanceProject.dto.BalanceSheetRow;
 import com.example.FinanceProject.dto.IncomeStatementRow;
 import com.example.FinanceProject.dto.RetainedEarningsReport;
 import com.example.FinanceProject.dto.TrialBalanceRow;
 import com.example.FinanceProject.entity.Account;
 import com.example.FinanceProject.entity.JournalEntryLine;
 import com.example.FinanceProject.entity.JournalStatus;
 import com.example.FinanceProject.entity.ReportSnapshot;
 import com.example.FinanceProject.repository.AccountRepo;
 import com.example.FinanceProject.repository.JournalEntryLineRepo;
 import com.example.FinanceProject.repository.JournalEntryRepo; // Import JournalEntryRepo
 import com.example.FinanceProject.repository.ReportSnapshotRepo;
 import com.fasterxml.jackson.core.JsonProcessingException;
 import com.fasterxml.jackson.core.type.TypeReference;
 import com.fasterxml.jackson.databind.ObjectMapper;

 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.data.domain.Sort;
 import org.springframework.stereotype.Service;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import static java.math.BigDecimal.ZERO;
 import java.time.LocalDate;
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Optional;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.stream.Collectors;


 @Service
 public class FinancialReportService {

     private static final Logger log = LoggerFactory.getLogger(FinancialReportService.class);

     private final AccountRepo accountRepo;
     private final ObjectMapper objectMapper;
     private final ReportSnapshotRepo snapRepo;
     private final AccountService accountService;
     private final JournalEntryLineRepo journalEntryLineRepo;
     private final JournalEntryRepo journalEntryRepo; // Inject JournalEntryRepo


     @Autowired
     public FinancialReportService(AccountRepo accountRepo,
                                   ReportSnapshotRepo snapRepo,
                                   ObjectMapper objectMapper,
                                   AccountService accountService,
                                   JournalEntryLineRepo journalEntryLineRepo,
                                   JournalEntryRepo journalEntryRepo) { // Add JournalEntryRepo
         this.accountRepo = accountRepo;
         this.snapRepo = snapRepo;
         this.objectMapper = objectMapper;
         this.accountService = accountService;
         this.journalEntryLineRepo = journalEntryLineRepo;
         this.journalEntryRepo = journalEntryRepo; // Initialize repo
     }

    // --- calculateBalanceAsOf (Unchanged) ---
    private BigDecimal calculateBalanceAsOf(Long accountId, LocalDate asOfDate) {
        Optional<Account> accountOpt = accountRepo.findById(accountId);
        if (!accountOpt.isPresent()) {
            log.error("Account not found for balance calculation: ID {}", accountId);
            return ZERO;
        }
        Account account = accountOpt.get();
        BigDecimal currentBalance = account.getInitialBalance() != null ? account.getInitialBalance() : ZERO;
        log.trace("Account {} Starting balance (Initial): {}", accountId, currentBalance);

        List<JournalEntryLine> lines = journalEntryLineRepo.findLinesForBalanceAsOf(accountId, asOfDate);
        log.trace("Found {} approved journal lines for account {} up to {}", lines.size(), accountId, asOfDate);

        for (JournalEntryLine line : lines) {
            BigDecimal debitChange = line.getDebit() != null ? line.getDebit() : ZERO;
            BigDecimal creditChange = line.getCredit() != null ? line.getCredit() : ZERO;
            BigDecimal balanceChange;

            if ("Debit".equalsIgnoreCase(account.getNormalSide())) {
                balanceChange = debitChange.subtract(creditChange);
            } else {
                balanceChange = creditChange.subtract(debitChange);
            }
            currentBalance = currentBalance.add(balanceChange);
            log.trace("JE Line {}: Debit={}, Credit={}, NormalSide={}, BalanceChange={}, NewBalance={}",
                     line.getId(), debitChange, creditChange, account.getNormalSide(), balanceChange, currentBalance);
        }
        log.debug("Calculated balance for Account {} ({}) as of {}: {}", account.getId(), account.getAccountName(), asOfDate, currentBalance);
        return currentBalance.setScale(2, RoundingMode.HALF_UP);
    }

    // --- calculatePeriodChange (Unchanged) ---
     private BigDecimal calculatePeriodChange(Long accountId, LocalDate start, LocalDate end) {
        Optional<Account> accountOpt = accountRepo.findById(accountId);
        if (!accountOpt.isPresent()) return ZERO;
        Account account = accountOpt.get();

        List<JournalEntryLine> lines = journalEntryLineRepo.findLinesForPeriod(accountId, start, end);
        BigDecimal netChange = ZERO;

        for (JournalEntryLine line : lines) {
             BigDecimal debitChange = line.getDebit() != null ? line.getDebit() : ZERO;
             BigDecimal creditChange = line.getCredit() != null ? line.getCredit() : ZERO;
             BigDecimal balanceChange;

             if ("Debit".equalsIgnoreCase(account.getNormalSide())) {
                 balanceChange = debitChange.subtract(creditChange);
             } else {
                 balanceChange = creditChange.subtract(debitChange);
             }
             netChange = netChange.add(balanceChange);
        }
        log.trace("Period change for Account {} ({}) from {} to {}: {}", account.getId(), account.getAccountName(), start, end, netChange);
        return netChange.setScale(2, RoundingMode.HALF_UP);
    }

    // --- Trial Balance (Unchanged) ---
    public List<TrialBalanceRow> buildTrialBalance(LocalDate start, LocalDate end) {
        // ... (keep existing implementation) ...
        log.warn("Building Trial Balance using STATIC balances from Chart of Accounts (as of last update), not calculated for period {} to {}", start, end);
         List<Account> accounts = accountRepo.findAll(Sort.by("accountOrder", "accountNumber"));
         List<TrialBalanceRow> rows = new ArrayList<>();
         boolean firstDebitFound = false;
         boolean firstCreditFound = false;

         for (Account acct : accounts) {
            BigDecimal endingBalance = calculateBalanceAsOf(acct.getId(), end);
             BigDecimal debitCol = ZERO;
             BigDecimal creditCol = ZERO;

             if ("Debit".equalsIgnoreCase(acct.getNormalSide())) {
                 if (endingBalance.signum() >= 0) debitCol = endingBalance;
                 else creditCol = endingBalance.abs();
             } else {
                 if (endingBalance.signum() >= 0) creditCol = endingBalance;
                 else debitCol = endingBalance.abs();
             }

             if (acct.isActive() || endingBalance.compareTo(ZERO) != 0) {
                  TrialBalanceRow row = new TrialBalanceRow(
                      acct.getId(),
                      acct.getAccountNumber(),
                      acct.getAccountName(),
                      debitCol,
                      creditCol
                  );
                  if (!firstDebitFound && debitCol.compareTo(ZERO) != 0) {
                      row.setShowDebitDollarSign(true);
                      firstDebitFound = true;
                  }
                  if (!firstCreditFound && creditCol.compareTo(ZERO) != 0) {
                      row.setShowCreditDollarSign(true);
                      firstCreditFound = true;
                  }
                  rows.add(row);
              }
         }
         saveSnapshot("TB", start, end, rows);
         return rows;
    }

    // --- Income Statement Internal (Dynamic - Unchanged) ---
     private List<IncomeStatementRow> buildIncomeStatementInternal(LocalDate start, LocalDate end) {
        // ... (keep existing implementation) ...
        List<IncomeStatementRow> rows = new ArrayList<>();
        Sort sortOrder = Sort.by("accountOrder", "accountName");
        boolean firstRevenueItemFound = false;
        boolean firstExpenseItemFound = false;

        rows.add(new IncomeStatementRow("Revenue")); // Header
        BigDecimal totalRevenue = ZERO;
        List<Account> revAccounts = accountRepo.findByAccountCategory("Revenue", sortOrder);
        for (Account a : revAccounts) {
            BigDecimal periodChange = calculatePeriodChange(a.getId(), start, end);
            if ("Debit".equalsIgnoreCase(a.getNormalSide())) { periodChange = periodChange.negate(); }

            if (periodChange.compareTo(ZERO) != 0 || a.isActive()) {
                 IncomeStatementRow row = new IncomeStatementRow(a.getId(), a.getAccountName(), periodChange);
                 if (!firstRevenueItemFound && periodChange.compareTo(ZERO) != 0) {
                     row.setShowDollarSign(true);
                     firstRevenueItemFound = true;
                 }
                 rows.add(row);
                 totalRevenue = totalRevenue.add(periodChange);
            }
        }
        IncomeStatementRow totalRevRow = new IncomeStatementRow("Total Revenue", totalRevenue);
        if (totalRevenue.compareTo(ZERO) != 0) totalRevRow.setShowDollarSign(true);
        rows.add(totalRevRow);
        rows.add(new IncomeStatementRow("")); // Blank Row

        rows.add(new IncomeStatementRow("Expenses")); // Header
        BigDecimal totalExpenses = ZERO;
        List<Account> expAccounts = accountRepo.findByAccountCategory("Expense", sortOrder);
        for (Account a : expAccounts) {
            BigDecimal periodChange = calculatePeriodChange(a.getId(), start, end);
             if ("Credit".equalsIgnoreCase(a.getNormalSide())) { periodChange = periodChange.negate(); }

             if (periodChange.compareTo(ZERO) != 0 || a.isActive()) {
                 IncomeStatementRow row = new IncomeStatementRow(a.getId(), a.getAccountName(), periodChange);
                 if (!firstExpenseItemFound && periodChange.compareTo(ZERO) != 0) {
                     row.setShowDollarSign(true);
                     firstExpenseItemFound = true;
                 }
                 rows.add(row);
                 totalExpenses = totalExpenses.add(periodChange);
            }
        }
        IncomeStatementRow totalExpRow = new IncomeStatementRow("Total Expenses", totalExpenses);
        if (totalExpenses.compareTo(ZERO) != 0) totalExpRow.setShowDollarSign(true);
        rows.add(totalExpRow);
        rows.add(new IncomeStatementRow("")); // Blank Row

        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        IncomeStatementRow netIncomeRow = new IncomeStatementRow("Net Income", netIncome);
        if (netIncome.compareTo(ZERO) != 0) netIncomeRow.setShowDollarSign(true);
        rows.add(netIncomeRow);

        return rows;
    }

    // --- Income Statement Public Method (Unchanged) ---
    public List<IncomeStatementRow> buildIncomeStatement(LocalDate start, LocalDate end) {
       // ... (keep existing implementation) ...
       log.info("Building Income Statement dynamically for period {} to {}", start, end);
        List<IncomeStatementRow> rows = buildIncomeStatementInternal(start, end);
        saveSnapshot("IS", start, end, rows);
        return rows;
    }

    // --- Balance Sheet Sorting Helpers (Unchanged) ---
    // ... (keep existing getAssetSubCategoryOrder, getLiabilitySubCategoryOrder, getEquitySubCategoryOrder) ...
    private int getAssetSubCategoryOrder(String subCategory) {
        if (subCategory == null) return 99;
        String lowerSubCategory = subCategory.trim().toLowerCase();
        return switch (lowerSubCategory) {
            case "current assets" -> 1;
            case "property plant & equipment" -> 2;
            case "accumulated depreciation" -> 3;
            default -> 99;
        };
    }
    private int getLiabilitySubCategoryOrder(String subCategory) {
         if (subCategory == null) return 99;
         String lowerSubCategory = subCategory.trim().toLowerCase();
         return switch (lowerSubCategory) {
             case "current liability" -> 1;
             default -> 99;
         };
     }
    private int getEquitySubCategoryOrder(String subCategory) {
         if (subCategory == null) return 99;
         String lowerSubCategory = subCategory.trim().toLowerCase();
         return switch (lowerSubCategory) {
             case "contributed capital", "common stock" -> 1;
             case "retained earnings" -> 2;
             default -> 99;
         };
     }

    // --- Balance Sheet (Explicit RE Calculation) ---
    public List<BalanceSheetRow> buildBalanceSheet(LocalDate asOfDate) {
        log.info("Building Balance Sheet dynamically using calculated balances as of {}", asOfDate);
        List<BalanceSheetRow> rows = new ArrayList<>();
        BigDecimal zero = ZERO;

        // --- Determine Calculation Start Date ---
        LocalDate systemStartDate = journalEntryRepo.findMinApprovedEntryDate()
                                        .orElse(asOfDate); // Default to asOfDate if no entries found
        log.info("Determined system start date for cumulative calculations: {}", systemStartDate);

        // --- Fetch and Sort Accounts ---
        List<Account> accounts = accountRepo.findByStatement("BS").stream()
            .sorted(Comparator
                .comparing(Account::getAccountCategory, Comparator.nullsLast((c1, c2) -> Map.of("Asset", 1, "Liability", 2, "Equity", 3).getOrDefault(c1, 99) - Map.of("Asset", 1, "Liability", 2, "Equity", 3).getOrDefault(c2, 99)))
                .thenComparing((Account a1, Account a2) -> {
                    String cat = a1.getAccountCategory();
                    if (cat == null) return 0;
                    if ("Asset".equalsIgnoreCase(cat)) return getAssetSubCategoryOrder(a1.getAccountSubcategory()) - getAssetSubCategoryOrder(a2.getAccountSubcategory());
                    if ("Liability".equalsIgnoreCase(cat)) return getLiabilitySubCategoryOrder(a1.getAccountSubcategory()) - getLiabilitySubCategoryOrder(a2.getAccountSubcategory());
                    if ("Equity".equalsIgnoreCase(cat)) return getEquitySubCategoryOrder(a1.getAccountSubcategory()) - getEquitySubCategoryOrder(a2.getAccountSubcategory());
                    return 0;
                })
                .thenComparing(Comparator.comparing(Account::getAccountOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .thenComparing(Comparator.comparing(Account::getAccountNumber, Comparator.nullsLast(Comparator.naturalOrder())))
            )
            .toList();

        log.info("Processing Balance Sheet for {} relevant accounts using calculated balances.", accounts.size());

        // --- Initialize Flags and Totals ---
        AtomicBoolean firstCurrentAssetFound = new AtomicBoolean(false);
        AtomicBoolean firstPPEAssetFound = new AtomicBoolean(false);
        AtomicBoolean firstAccumDepFound = new AtomicBoolean(false);
        AtomicBoolean firstCurrentLiabilityFound = new AtomicBoolean(false);
        AtomicBoolean firstOtherLiabilityFound = new AtomicBoolean(false);
        AtomicBoolean firstContributedCapitalFound = new AtomicBoolean(false);
        AtomicBoolean firstRetainedEarningsFound = new AtomicBoolean(false); // Keep flag for dollar sign

        java.util.function.BiConsumer<AtomicBoolean, BalanceSheetRow> checkAndSetFirst = (flag, row) -> {
            if (!flag.get() && row.getAmount() != null && row.getAmount().compareTo(zero) != 0) {
                row.setShowDollarSign(true);
                flag.set(true);
            }
        };

        BigDecimal totalCurrentAssets = zero;
        BigDecimal totalPPE = zero;
        BigDecimal totalAccumDepMagnitude = zero;
        BigDecimal totalCurrentLiabilities = zero;
        BigDecimal totalOtherLiabilities = zero;
        BigDecimal contributedCapitalTotal = zero;
        // Retained Earnings components will be calculated explicitly

        // --- Process Assets ---
        rows.add(new BalanceSheetRow(null, "Assets", null, 0, "section-header bold", false));
        // ... (Current Assets processing remains the same, using calculateBalanceAsOf) ...
        rows.add(new BalanceSheetRow(null, "Current Assets", null, 0, "subheader indent-1", false));
        List<Account> currentAssetAccounts = accounts.stream()
            .filter(a -> "Asset".equalsIgnoreCase(a.getAccountCategory()) && "Current Assets".equalsIgnoreCase(a.getAccountSubcategory()))
            .toList();
        for (int i = 0; i < currentAssetAccounts.size(); i++) {
            Account account = currentAssetAccounts.get(i);
            BigDecimal balance = calculateBalanceAsOf(account.getId(), asOfDate);
            if (balance.compareTo(ZERO) != 0 || account.isActive()) {
                totalCurrentAssets = totalCurrentAssets.add(balance);
                String style = "account indent-2" + (i == currentAssetAccounts.size() - 1 ? " underline" : "");
                BalanceSheetRow row = new BalanceSheetRow(account.getId(), account.getAccountName(), balance, 1, style, false);
                checkAndSetFirst.accept(firstCurrentAssetFound, row);
                rows.add(row);
            }
        }
        rows.add(new BalanceSheetRow(null, "Total Current Assets", totalCurrentAssets, 2, "subtotal indent-1 bold", true));

        // ... (PPE and AD processing remains the same as previous version) ...
         rows.add(new BalanceSheetRow(null, "Property Plant & Equipment", null, 0, "subheader indent-1", false));
        List<Account> ppeAssetAccounts = accounts.stream()
            .filter(a -> "Asset".equalsIgnoreCase(a.getAccountCategory()) && "Property Plant & Equipment".equalsIgnoreCase(a.getAccountSubcategory()))
            .toList();
        List<Account> accumDepAccounts = accounts.stream()
             .filter(a -> "Asset".equalsIgnoreCase(a.getAccountCategory())
                       && ( "Accumulated Depreciation".equalsIgnoreCase(a.getAccountSubcategory())
                            || a.getAccountName().toLowerCase().contains("accumulated depreciation") )
                    )
             .toList();
         log.info("Found {} Accumulated Depreciation accounts to process.", accumDepAccounts.size()); // Log how many AD accounts found

        for (Account account : ppeAssetAccounts) {
            BigDecimal balance = calculateBalanceAsOf(account.getId(), asOfDate);
            if (balance.compareTo(ZERO) != 0 || account.isActive()) {
                totalPPE = totalPPE.add(balance);
                BalanceSheetRow row = new BalanceSheetRow(account.getId(), account.getAccountName(), balance, 1, "account indent-2", false);
                checkAndSetFirst.accept(firstPPEAssetFound, row);
                rows.add(row);
            }
        }
        for (int i = 0; i < accumDepAccounts.size(); i++) {
            Account account = accumDepAccounts.get(i);
            BigDecimal calculatedBalance = calculateBalanceAsOf(account.getId(), asOfDate);

            if (calculatedBalance.compareTo(ZERO) != 0 || account.isActive()) {
                 BigDecimal displayAmount = calculatedBalance.negate();
                 BigDecimal reductionMagnitude = calculatedBalance.abs();
                 totalAccumDepMagnitude = totalAccumDepMagnitude.add(reductionMagnitude);

                 String style = "account indent-2" + (i == accumDepAccounts.size() - 1 ? " underline" : "");
                 BalanceSheetRow row = new BalanceSheetRow(account.getId(), account.getAccountName(), displayAmount, 1, style, false);
                 checkAndSetFirst.accept(firstAccumDepFound, row);
                 rows.add(row);
                 log.debug("Added Accum Dep: {} - Calculated Balance: {}, Display Amount: {}, Reduction: {}",
                           account.getAccountName(), calculatedBalance, displayAmount, reductionMagnitude);
             }
        }
        BigDecimal netPPE = totalPPE.subtract(totalAccumDepMagnitude);
        rows.add(new BalanceSheetRow(null, "Property Plant & Equipment, Net", netPPE, 2, "subtotal indent-1 bold underline", true));

        BigDecimal totalAssets = totalCurrentAssets.add(netPPE);
        rows.add(new BalanceSheetRow(null, "Total Assets", totalAssets, 2, "total bold double-underline", true));
        rows.add(new BalanceSheetRow()); // Blank Row

        // --- Process Liabilities ---
        rows.add(new BalanceSheetRow(null, "Liabilities & Stockholders' Equity", null, 0, "section-header bold", false));
        rows.add(new BalanceSheetRow(null, "Liabilities", null, 0, "subheader indent-1", false));
        // ... (Current Liabilities processing remains the same, using calculateBalanceAsOf) ...
        rows.add(new BalanceSheetRow(null, "Current Liabilities", null, 0, "subheader indent-2", false));
        List<Account> currentLiabilityAccounts = accounts.stream()
            .filter(a -> "Liability".equalsIgnoreCase(a.getAccountCategory()) && "Current Liability".equalsIgnoreCase(a.getAccountSubcategory()))
            .toList();
        for (int i = 0; i < currentLiabilityAccounts.size(); i++) {
            Account account = currentLiabilityAccounts.get(i);
            BigDecimal balance = calculateBalanceAsOf(account.getId(), asOfDate);
            if (balance.compareTo(ZERO) != 0 || account.isActive()) {
                totalCurrentLiabilities = totalCurrentLiabilities.add(balance);
                String style = "account indent-3" + (i == currentLiabilityAccounts.size() - 1 ? " underline" : "");
                BalanceSheetRow row = new BalanceSheetRow(account.getId(), account.getAccountName(), balance, 1, style, false);
                checkAndSetFirst.accept(firstCurrentLiabilityFound, row);
                rows.add(row);
            }
        }
        rows.add(new BalanceSheetRow(null, "Total Current Liabilities", totalCurrentLiabilities, 2, "subtotal indent-2 bold", true));

        // ... (Other Liabilities processing remains the same, using calculateBalanceAsOf) ...
        List<Account> otherLiabilityAccounts = accounts.stream()
            .filter(a -> "Liability".equalsIgnoreCase(a.getAccountCategory()) && !"Current Liability".equalsIgnoreCase(a.getAccountSubcategory()))
            .toList();
        for (int i = 0; i < otherLiabilityAccounts.size(); i++) {
             Account account = otherLiabilityAccounts.get(i);
             BigDecimal balance = calculateBalanceAsOf(account.getId(), asOfDate);
             if (balance.compareTo(ZERO) != 0 || account.isActive()) {
                 totalOtherLiabilities = totalOtherLiabilities.add(balance);
                 String style = "account indent-2" + (i == otherLiabilityAccounts.size() - 1 ? " underline" : "");
                 BalanceSheetRow row = new BalanceSheetRow(account.getId(), account.getAccountName(), balance, 2, style, false);
                 checkAndSetFirst.accept(firstOtherLiabilityFound, row);
                 rows.add(row);
             }
        }
        BigDecimal totalLiabilities = totalCurrentLiabilities.add(totalOtherLiabilities);
        rows.add(new BalanceSheetRow(null, "Total Liabilities", totalLiabilities, 2, "total indent-1 bold underline", true));

        // --- Process Equity (Explicit RE Calculation) ---
        rows.add(new BalanceSheetRow(null, "Stockholders' Equity", null, 0, "subheader indent-1", false));
        Account reAccount = null; // To store the RE account entity

        // Process Contributed Capital first
        List<Account> equityAccounts = accounts.stream()
            .filter(a -> "Equity".equalsIgnoreCase(a.getAccountCategory()))
            .toList(); // Already sorted

        for (Account account : equityAccounts) {
            String subcatLower = account.getAccountSubcategory() != null ? account.getAccountSubcategory().trim().toLowerCase() : "";
            String nameLower = account.getAccountName().trim().toLowerCase();

            if ("retained earnings".equals(subcatLower) || "retained earnings".equals(nameLower)) {
                 reAccount = account; // Found RE account, process later
            } else { // Process Contributed Capital / Other Equity using dynamic balance
                 BigDecimal balance = calculateBalanceAsOf(account.getId(), asOfDate);
                 if (balance.compareTo(ZERO) != 0 || account.isActive()) {
                     contributedCapitalTotal = contributedCapitalTotal.add(balance);
                     BalanceSheetRow row = new BalanceSheetRow(account.getId(), account.getAccountName(), balance, 2, "account indent-2", false);
                     checkAndSetFirst.accept(firstContributedCapitalFound, row);
                     rows.add(row);
                 }
            }
        }

        // *** Calculate Retained Earnings Explicitly ***
        BigDecimal retainedEarningsBalance;
        if (reAccount != null) {
            // 1. Get Initial Balance of RE account
            BigDecimal beginningBalance = reAccount.getInitialBalance() != null ? reAccount.getInitialBalance() : ZERO;
            log.info("RE Calculation: Initial Balance = {}", beginningBalance);

            // 2. Calculate Cumulative Net Income (Revenue - Expenses) from system start to asOfDate
            BigDecimal cumulativeRevenue = ZERO;
            for(Account acc : accountRepo.findByAccountCategory("Revenue", null)) { // Get all revenue accounts
                BigDecimal change = calculatePeriodChange(acc.getId(), systemStartDate, asOfDate);
                if ("Debit".equalsIgnoreCase(acc.getNormalSide())) change = change.negate();
                cumulativeRevenue = cumulativeRevenue.add(change);
            }
            BigDecimal cumulativeExpense = ZERO;
             for(Account acc : accountRepo.findByAccountCategory("Expense", null)) { // Get all expense accounts
                BigDecimal change = calculatePeriodChange(acc.getId(), systemStartDate, asOfDate);
                if ("Credit".equalsIgnoreCase(acc.getNormalSide())) change = change.negate();
                cumulativeExpense = cumulativeExpense.add(change);
            }
            BigDecimal cumulativeNetIncome = cumulativeRevenue.subtract(cumulativeExpense);
            log.info("RE Calculation: Cumulative NI (Rev={}, Exp={}) = {}", cumulativeRevenue, cumulativeExpense, cumulativeNetIncome);

             // 3. Calculate Cumulative Dividends from system start to asOfDate
             BigDecimal cumulativeDividends = ZERO;
             List<Account> dividendAccounts = accountRepo.findByAccountCategory("Equity", null).stream()
                  .filter(a -> a.getAccountName().toLowerCase().contains("dividend")) // Simple check
                  .toList();
              for(Account acc : dividendAccounts) {
                  BigDecimal change = calculatePeriodChange(acc.getId(), systemStartDate, asOfDate);
                   // Dividends decrease RE (Normal Debit typically)
                   if ("Credit".equalsIgnoreCase(acc.getNormalSide())) change = change.negate();
                   cumulativeDividends = cumulativeDividends.add(change); // Add the change representing deduction
             }
             log.info("RE Calculation: Cumulative Dividends = {}", cumulativeDividends);

            // 4. Final RE Balance
            retainedEarningsBalance = beginningBalance.add(cumulativeNetIncome).subtract(cumulativeDividends);
            retainedEarningsBalance = retainedEarningsBalance.setScale(2, RoundingMode.HALF_UP); // Ensure rounding
            log.info("RE Calculation: Final Balance = ({} + {} - {}) = {}", beginningBalance, cumulativeNetIncome, cumulativeDividends, retainedEarningsBalance);

            // Add the RE row to the report
            BalanceSheetRow reRow = new BalanceSheetRow(reAccount.getId(), reAccount.getAccountName(), retainedEarningsBalance, 2, "account indent-2 underline", false);
            checkAndSetFirst.accept(firstRetainedEarningsFound, reRow);
            rows.add(reRow);

        } else {
            log.error("Retained Earnings account could not be found for Balance Sheet calculation. RE will be zero.");
            retainedEarningsBalance = ZERO; // Set to zero if account not found
             // Optionally add a placeholder row:
             rows.add(new BalanceSheetRow(null, "Retained Earnings (Not Found)", ZERO, 2, "account indent-2 underline error", false));
        }

        // --- Total Equity & L+E ---
        BigDecimal totalEquity = contributedCapitalTotal.add(retainedEarningsBalance);
        rows.add(new BalanceSheetRow(null, "Total Stockholders' Equity", totalEquity, 2, "total indent-1 bold underline", true));

        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        rows.add(new BalanceSheetRow(null, "Total Liabilities & Stockholders' Equity", totalLiabilitiesAndEquity, 2, "total bold double-underline", true));

        // --- Verification Logging (Error Row Removed) ---
        if (totalAssets.setScale(2, RoundingMode.HALF_UP).compareTo(totalLiabilitiesAndEquity.setScale(2, RoundingMode.HALF_UP)) != 0) {
             BigDecimal difference = totalAssets.subtract(totalLiabilitiesAndEquity).setScale(2, RoundingMode.HALF_UP);
             log.error("BALANCE SHEET DOES NOT BALANCE! Date={}, Assets={}, Liab+Equity={}, Diff={}", asOfDate, totalAssets, totalLiabilitiesAndEquity, difference);
        } else {
             log.info("Balance Sheet BALANCES. Date={}, Total={}", asOfDate, totalAssets);
        }

        saveSnapshot("BS", asOfDate, asOfDate, rows);
        return rows;
    }


    // --- Retained Earnings Report (Dynamic - Unchanged) ---
    public RetainedEarningsReport buildRetainedEarnings(LocalDate start, LocalDate end) {
        // ... (keep existing implementation - it already uses the formula correctly for the specified period) ...
        log.info("Building Retained Earnings statement dynamically for period {} to {}", start, end);
         RetainedEarningsReport report = buildRetainedEarningsInternal(start, end);
         saveSnapshot("RE", start, end, report);
         return report;
    }

    // --- Retained Earnings Internal Calculation (Dynamic - Unchanged) ---
    private RetainedEarningsReport buildRetainedEarningsInternal(LocalDate start, LocalDate end) {
        // ... (keep existing implementation) ...
        Account reAccount = accountRepo.findByAccountCategory("Equity", null).stream()
            .filter(a -> "Retained Earnings".equalsIgnoreCase(a.getAccountSubcategory()) || "Retained Earnings".equalsIgnoreCase(a.getAccountName()))
            .findFirst()
            .orElse(null);

        if (reAccount == null) {
            log.error("Retained Earnings account not found. Cannot calculate statement.");
            return new RetainedEarningsReport(ZERO, ZERO, ZERO, ZERO, start, end);
        }

        BigDecimal beginningBalance = calculateBalanceAsOf(reAccount.getId(), start.minusDays(1));
        log.debug("Calculated Beginning RE as of {}: {}", start.minusDays(1), beginningBalance);

        BigDecimal totalRevenueChange = ZERO;
        List<Account> revAccounts = accountRepo.findByAccountCategory("Revenue", null);
        for(Account acc : revAccounts) {
             BigDecimal change = calculatePeriodChange(acc.getId(), start, end);
             if ("Debit".equalsIgnoreCase(acc.getNormalSide())) change = change.negate();
             totalRevenueChange = totalRevenueChange.add(change);
        }
        BigDecimal totalExpenseChange = ZERO;
        List<Account> expAccounts = accountRepo.findByAccountCategory("Expense", null);
        for(Account acc : expAccounts) {
            BigDecimal change = calculatePeriodChange(acc.getId(), start, end);
            if ("Credit".equalsIgnoreCase(acc.getNormalSide())) change = change.negate();
            totalExpenseChange = totalExpenseChange.add(change);
        }
        BigDecimal netIncome = totalRevenueChange.subtract(totalExpenseChange);
        log.debug("Calculated Net Income for period {} to {}: {}", start, end, netIncome);

        BigDecimal dividends = ZERO;
        List<Account> dividendAccounts = accountRepo.findByAccountCategory("Equity", null).stream()
             .filter(a -> a.getAccountName().toLowerCase().contains("dividend"))
             .toList();
        for(Account acc : dividendAccounts) {
             BigDecimal change = calculatePeriodChange(acc.getId(), start, end);
              if ("Credit".equalsIgnoreCase(acc.getNormalSide())) change = change.negate();
              dividends = dividends.add(change);
        }
        log.debug("Calculated Dividends for period {} to {}: {}", start, end, dividends);

        BigDecimal endingBalance = beginningBalance.add(netIncome).subtract(dividends);
        log.debug("Calculated Ending RE as of {} using formula: {}", end, endingBalance);

        BigDecimal verificationBalance = calculateBalanceAsOf(reAccount.getId(), end);
        if (endingBalance.setScale(2, RoundingMode.HALF_UP).compareTo(verificationBalance.setScale(2, RoundingMode.HALF_UP)) != 0) {
             log.warn("Retained Earnings calculation mismatch! Formula Result: {}, Direct Calc: {}. Difference: {}",
                endingBalance, verificationBalance, endingBalance.subtract(verificationBalance));
        }

        return new RetainedEarningsReport(beginningBalance, netIncome, dividends, endingBalance.setScale(2, RoundingMode.HALF_UP), start, end);
    }


    // --- Snapshot methods (Unchanged) ---
    // ... (saveSnapshot, load methods) ...
    public <T> void saveSnapshot(String type, LocalDate s, LocalDate e, T payload) {
        ReportSnapshot ss = new ReportSnapshot();
        ss.setReportType(type);
        ss.setStartDate(s);
        ss.setEndDate(e);
        try {
             ss.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            log.error("Error serializing report payload for type {}: {}", type, ex.getMessage(), ex);
            throw new RuntimeException("Could not serialize report payload", ex);
        }
        snapRepo.save(ss);
        log.info("Saved {} snapshot for dates {} to {}", type, s, e);
    }
    public List<TrialBalanceRow> loadTrialBalanceFromSnapshot(Long snapshotId) throws Exception {
        ReportSnapshot ss = snapRepo.findById(snapshotId)
            .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));
        if (!"TB".equals(ss.getReportType())) {
            throw new IllegalArgumentException("Snapshot is not a Trial Balance");
        }
        return objectMapper.readValue(ss.getPayloadJson(), new TypeReference<List<TrialBalanceRow>>() {});
    }
    public List<IncomeStatementRow> loadIncomeStatementFromSnapshot(Long snapshotId) throws Exception {
        ReportSnapshot ss = snapRepo.findById(snapshotId)
            .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));
        if (!"IS".equals(ss.getReportType())) {
            throw new IllegalArgumentException("Snapshot is not an Income Statement");
        }
        return objectMapper.readValue(ss.getPayloadJson(), new TypeReference<List<IncomeStatementRow>>() {});
    }
    public List<BalanceSheetRow> loadBalanceSheetFromSnapshot(Long snapshotId) throws Exception {
        ReportSnapshot ss = snapRepo.findById(snapshotId)
            .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));
        if (!"BS".equals(ss.getReportType())) {
            throw new IllegalArgumentException("Snapshot is not a Balance Sheet");
        }
        return objectMapper.readValue(ss.getPayloadJson(), new TypeReference<List<BalanceSheetRow>>() {});
    }
    public RetainedEarningsReport loadRetainedEarningsFromSnapshot(Long snapshotId) throws Exception {
        ReportSnapshot ss = snapRepo.findById(snapshotId)
            .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));
        if (!"RE".equals(ss.getReportType())) {
            throw new IllegalArgumentException("Snapshot is not a Retained Earnings statement");
        }
        return objectMapper.readValue(ss.getPayloadJson(), RetainedEarningsReport.class);
    }
 }