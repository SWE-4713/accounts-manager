package com.example.FinanceProject.service;

 import com.example.FinanceProject.dto.BalanceSheetRow;
 import com.example.FinanceProject.dto.IncomeStatementRow;
 import com.example.FinanceProject.dto.RetainedEarningsReport;
 import com.example.FinanceProject.dto.TrialBalanceRow;
 import com.example.FinanceProject.entity.Account;
 import com.example.FinanceProject.entity.ReportSnapshot;
 import com.example.FinanceProject.repository.AccountRepo;
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
 import java.time.LocalDate;
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.stream.Collectors;


 @Service
 public class FinancialReportService {

     private static final Logger log = LoggerFactory.getLogger(FinancialReportService.class);

     private final AccountRepo accountRepo;
     private final ObjectMapper objectMapper;
     private final ReportSnapshotRepo snapRepo;

     @Autowired
     public FinancialReportService(AccountRepo accountRepo,
                                   ReportSnapshotRepo snapRepo,
                                   ObjectMapper objectMapper) {
         this.accountRepo = accountRepo;
         this.snapRepo = snapRepo;
         this.objectMapper = objectMapper;
     }


    // --- Trial Balance (Uses Static CoA Balance) ---
    public List<TrialBalanceRow> buildTrialBalance(LocalDate start, LocalDate end) {
        log.warn("Building Trial Balance using STATIC balances from Chart of Accounts (as of last update), not calculated for period {} to {}", start, end);
        List<Account> accounts = accountRepo.findAll(Sort.by("accountOrder", "accountNumber"));
        List<TrialBalanceRow> rows = new ArrayList<>();

        for (Account acct : accounts) {
            BigDecimal endingBalance = acct.getBalance() != null ? acct.getBalance() : BigDecimal.ZERO;
            BigDecimal debitCol = BigDecimal.ZERO;
            BigDecimal creditCol = BigDecimal.ZERO;

            if ("Debit".equalsIgnoreCase(acct.getNormalSide())) {
                if (endingBalance.signum() >= 0) debitCol = endingBalance;
                else creditCol = endingBalance.abs();
            } else {
                if (endingBalance.signum() >= 0) creditCol = endingBalance;
                else debitCol = endingBalance.abs();
            }

            if (acct.isActive() || endingBalance.compareTo(BigDecimal.ZERO) != 0) {
                 rows.add(new TrialBalanceRow(acct.getAccountNumber(), acct.getAccountName(), debitCol, creditCol));
             }
        }
        saveSnapshot("TB", start, end, rows);
        return rows;
    }


    // --- Income Statement (Uses Static CoA Balance - NON-STANDARD) ---
    public List<IncomeStatementRow> buildIncomeStatement(LocalDate start, LocalDate end) {
        log.warn("Building Income Statement using STATIC balances from Chart of Accounts. This is NON-STANDARD and does NOT reflect activity for the period {} to {}.", start, end);
        List<IncomeStatementRow> rows = buildIncomeStatementInternal(start, end);
        saveSnapshot("IS", start, end, rows);
        return rows;
    }

    private List<IncomeStatementRow> buildIncomeStatementInternal(LocalDate start, LocalDate end) {
        List<IncomeStatementRow> rows = new ArrayList<>();
        BigDecimal zero = BigDecimal.ZERO;
        Sort sortOrder = Sort.by("accountOrder", "accountName");

        rows.add(new IncomeStatementRow("Revenue", null));
        BigDecimal totalRevenue = zero;
        List<Account> revAccounts = accountRepo.findByAccountCategory("Revenue", sortOrder);
        for (Account a : revAccounts) {
            BigDecimal amt = a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO;
            if ("Debit".equalsIgnoreCase(a.getNormalSide())) { amt = amt.negate(); }
            rows.add(new IncomeStatementRow(a.getAccountName(), amt));
            totalRevenue = totalRevenue.add(amt);
        }
        rows.add(new IncomeStatementRow("Total Revenue", totalRevenue));
        rows.add(new IncomeStatementRow("", null)); // Blank Row

        rows.add(new IncomeStatementRow("Expenses", null));
        BigDecimal totalExpenses = zero;
        List<Account> expAccounts = accountRepo.findByAccountCategory("Expense", sortOrder);
        for (Account a : expAccounts) {
            BigDecimal amt = a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO;
            if ("Credit".equalsIgnoreCase(a.getNormalSide())) { amt = amt.negate(); }
            rows.add(new IncomeStatementRow(a.getAccountName(), amt));
            totalExpenses = totalExpenses.add(amt);
        }
        rows.add(new IncomeStatementRow("Total Expenses", totalExpenses));
        rows.add(new IncomeStatementRow("", null)); // Blank Row

        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        rows.add(new IncomeStatementRow("Net Income", netIncome));

        return rows;
    }


    // --- Balance Sheet (Uses Static CoA Balance) ---
    public List<BalanceSheetRow> buildBalanceSheet(LocalDate asOfDate) {
        log.info("Building Balance Sheet using STATIC balances from Chart of Accounts (as of last update), for date label {}", asOfDate);
        List<BalanceSheetRow> rows = new ArrayList<>();

        // Fetch all accounts marked for Balance Sheet
        List<Account> accounts = accountRepo.findByStatement("BS").stream()
             .filter(a -> a.isActive() || (a.getBalance() != null && a.getBalance().compareTo(BigDecimal.ZERO) != 0))
             .sorted(Comparator
                 // Primary sort: Category Order
                 .comparing(Account::getAccountCategory, Comparator.nullsLast((c1, c2) -> {
                     Map<String, Integer> categoryOrder = Map.of("Asset", 1, "Liability", 2, "Equity", 3);
                     return categoryOrder.getOrDefault(c1, 99) - categoryOrder.getOrDefault(c2, 99);
                 }))
                 // Secondary sort: SubCategory Order (within Category)
                 .thenComparing((Account a1, Account a2) -> { // Explicit types needed here
                     String cat = a1.getAccountCategory();
                     if ("Asset".equalsIgnoreCase(cat)) {
                         return getAssetSubCategoryOrder(a1.getAccountSubcategory()) - getAssetSubCategoryOrder(a2.getAccountSubcategory());
                     } else if ("Liability".equalsIgnoreCase(cat)) {
                         return getLiabilitySubCategoryOrder(a1.getAccountSubcategory()) - getLiabilitySubCategoryOrder(a2.getAccountSubcategory());
                     } else if ("Equity".equalsIgnoreCase(cat)) {
                         return getEquitySubCategoryOrder(a1.getAccountSubcategory()) - getEquitySubCategoryOrder(a2.getAccountSubcategory());
                     }
                     return 0;
                 })
                 // Tertiary sort: Account Order (within SubCategory)
                 .thenComparing(Comparator.comparing(Account::getAccountOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                 // Quaternary sort: Account Number (within Account Order)
                 .thenComparing(Comparator.comparing(Account::getAccountNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                )
             .collect(Collectors.toList());

        log.info("Processing Balance Sheet for {} relevant accounts using static CoA balances.", accounts.size());

        // --- Assets ---
        rows.add(new BalanceSheetRow("Assets", null, 0, "section-header bold", false));
        BigDecimal totalCurrentAssets = BigDecimal.ZERO;
        BigDecimal totalPPE = BigDecimal.ZERO;
        BigDecimal totalAccumDep = BigDecimal.ZERO;
        boolean firstCurrentAsset = true;
        boolean firstPPEAsset = true;
        boolean firstAccumDep = true;

        // Process Current Assets
        rows.add(new BalanceSheetRow("Current Assets", null, 0, "subheader indent-1", false));
        List<Account> currentAssetAccounts = accounts.stream()
            .filter(a -> "Asset".equalsIgnoreCase(a.getAccountCategory()) && "Current Assets".equalsIgnoreCase(a.getAccountSubcategory()))
            .collect(Collectors.toList());
        // *** ADDED LOGGING ***
        log.debug("Found {} accounts potentially matching Current Assets based on Category/Subcategory.", currentAssetAccounts.size());
        if (currentAssetAccounts.isEmpty()) {
             log.warn("No accounts found with Category='Asset' AND Subcategory='Current Assets'. Check CoA data and subcategory values.");
        }
        for (int i = 0; i < currentAssetAccounts.size(); i++) {
            Account account = currentAssetAccounts.get(i);
            // *** USE STATIC BALANCE ***
            BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            totalCurrentAssets = totalCurrentAssets.add(balance);
            String style = "account indent-2";
            if (i == currentAssetAccounts.size() - 1) { style += " underline"; }
            rows.add(new BalanceSheetRow(account.getAccountName(), balance, 1, style, firstCurrentAsset));
            firstCurrentAsset = false;
            // *** ADDED LOGGING ***
            log.debug("-> Added Current Asset Row: Name='{}', Balance={}, Style='{}'", account.getAccountName(), balance, style);
        }
        rows.add(new BalanceSheetRow("Total Current Assets", totalCurrentAssets, 2, "subtotal indent-1 bold", true));

        // Process Property, Plant & Equipment
        rows.add(new BalanceSheetRow("Property Plant & Equipment", null, 0, "subheader indent-1", false));
        List<Account> ppeAssetAccounts = accounts.stream()
            .filter(a -> "Asset".equalsIgnoreCase(a.getAccountCategory()) && "Property Plant & Equipment".equalsIgnoreCase(a.getAccountSubcategory()))
            .collect(Collectors.toList());
        List<Account> accumDepAccounts = accounts.stream()
            .filter(a -> "Asset".equalsIgnoreCase(a.getAccountCategory()) && "Accumulated Depreciation".equalsIgnoreCase(a.getAccountSubcategory()))
            .collect(Collectors.toList());

        for (Account account : ppeAssetAccounts) {
            BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            totalPPE = totalPPE.add(balance);
            String style = "account indent-2";
            rows.add(new BalanceSheetRow(account.getAccountName(), balance, 1, style, firstPPEAsset));
            firstPPEAsset = false;
            log.debug("Added PPE Asset: {} - Static Balance: {}", account.getAccountName(), balance);
        }
        for (int i = 0; i < accumDepAccounts.size(); i++) {
            Account account = accumDepAccounts.get(i);
            BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            totalAccumDep = totalAccumDep.add(balance);
            String style = "account indent-2";
            if (i == accumDepAccounts.size() - 1) { style += " underline"; }
            rows.add(new BalanceSheetRow(account.getAccountName(), balance, 1, style, firstAccumDep));
            firstAccumDep = false;
            log.debug("Added Accum Dep: {} - Static Balance: {}", account.getAccountName(), balance);
        }
        BigDecimal netPPE = totalPPE.add(totalAccumDep);
        rows.add(new BalanceSheetRow("Property Plant & Equipment, Net", netPPE, 2, "subtotal indent-1 bold underline", true));

        // --- Total Assets ---
        BigDecimal totalAssets = totalCurrentAssets.add(netPPE);
        rows.add(new BalanceSheetRow("Total Assets", totalAssets, 2, "total bold double-underline", true));
        rows.add(new BalanceSheetRow()); // Blank Row

        // --- Liabilities & Stockholders' Equity ---
        rows.add(new BalanceSheetRow("Liabilities & Stockholders' Equity", null, 0, "section-header bold", false));
        rows.add(new BalanceSheetRow("Liabilities", null, 0, "subheader indent-1", false));
        BigDecimal totalCurrentLiabilities = BigDecimal.ZERO;
        BigDecimal totalOtherLiabilities = BigDecimal.ZERO;
        boolean firstCurrentLiability = true;
        boolean firstOtherLiability = true;

        // Process Current Liabilities
        rows.add(new BalanceSheetRow("Current Liabilities", null, 0, "subheader indent-2", false));
        List<Account> currentLiabilityAccounts = accounts.stream()
            .filter(a -> "Liability".equalsIgnoreCase(a.getAccountCategory()) && "Current Liability".equalsIgnoreCase(a.getAccountSubcategory()))
            .collect(Collectors.toList());
        for (int i = 0; i < currentLiabilityAccounts.size(); i++) {
            Account account = currentLiabilityAccounts.get(i);
            BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            totalCurrentLiabilities = totalCurrentLiabilities.add(balance);
            String style = "account indent-3";
            if (i == currentLiabilityAccounts.size() - 1) { style += " underline"; }
            rows.add(new BalanceSheetRow(account.getAccountName(), balance, 1, style, firstCurrentLiability));
            firstCurrentLiability = false;
            log.debug("Added Current Liability: {} - Static Balance: {}", account.getAccountName(), balance);
        }
        rows.add(new BalanceSheetRow("Total Current Liabilities", totalCurrentLiabilities, 2, "subtotal indent-2 bold", true));

        // Process Other/Long-Term Liabilities
        List<Account> otherLiabilityAccounts = accounts.stream()
            .filter(a -> "Liability".equalsIgnoreCase(a.getAccountCategory()) && !"Current Liability".equalsIgnoreCase(a.getAccountSubcategory()))
             .sorted(Comparator.comparing((Account a) -> getLiabilitySubCategoryOrder(a.getAccountSubcategory())) // Ensure correct type here too
                               .thenComparing(Account::getAccountOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                               .thenComparing(Account::getAccountNumber, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
        for (int i = 0; i < otherLiabilityAccounts.size(); i++) {
             Account account = otherLiabilityAccounts.get(i);
             BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
             totalOtherLiabilities = totalOtherLiabilities.add(balance);
             String style = "account indent-2";
             if (i == otherLiabilityAccounts.size() - 1) { style += " underline"; }
             rows.add(new BalanceSheetRow(account.getAccountName(), balance, 2, style, firstOtherLiability));
             firstOtherLiability = false;
             log.debug("Added Other Liability: {} - Static Balance: {}", account.getAccountName(), balance);
        }

        // --- Total Liabilities ---
        BigDecimal totalLiabilities = totalCurrentLiabilities.add(totalOtherLiabilities);
        rows.add(new BalanceSheetRow("Total Liabilities", totalLiabilities, 2, "total indent-1 bold underline", true));

        // --- Stockholders' Equity ---
        rows.add(new BalanceSheetRow("Stockholders' Equity", null, 0, "subheader indent-1", false));
        BigDecimal contributedCapitalTotal = BigDecimal.ZERO;
        Account retainedEarningsAccount = null;
        boolean firstEquityItem = true;

        // Process Contributed Capital accounts
        List<Account> contributedCapitalAccounts = accounts.stream()
            .filter(a -> "Equity".equalsIgnoreCase(a.getAccountCategory()) &&
                         ("Contributed Capital".equalsIgnoreCase(a.getAccountSubcategory()) ||
                          a.getAccountName().toLowerCase().contains("common stock") ||
                          a.getAccountName().toLowerCase().contains("contributed capital")))
             .sorted(Comparator.comparing((Account a) -> getEquitySubCategoryOrder(a.getAccountSubcategory())) // Ensure correct type here too
                              .thenComparing(Account::getAccountOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                              .thenComparing(Account::getAccountNumber, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());

        // *** ADDED LOGGING ***
        log.debug("Found {} accounts potentially matching Contributed Capital based on filter.", contributedCapitalAccounts.size());
        if (contributedCapitalAccounts.isEmpty()) {
             log.warn("No accounts found for Contributed Capital based on Category/Subcategory/Name. Check CoA data.");
        }
        for(Account account : contributedCapitalAccounts) {
             // *** USE STATIC BALANCE ***
             BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
             // *** ADDED LOGGING ***
             log.debug("-> Adding to Contributed Capital: Account='{}' ({}), Balance={}", account.getAccountName(), account.getAccountNumber(), balance);
             contributedCapitalTotal = contributedCapitalTotal.add(balance);
        }
        String contributedCapitalLabel = contributedCapitalAccounts.isEmpty() ? "Contributed Capital" : contributedCapitalAccounts.get(0).getAccountName();
        // *** ADDED LOGGING ***
        log.debug("Final Contributed Capital Total: {}", contributedCapitalTotal);
        rows.add(new BalanceSheetRow(contributedCapitalLabel, contributedCapitalTotal, 2, "account indent-2", firstEquityItem));
        firstEquityItem = false;


        // Find Retained Earnings Account
        retainedEarningsAccount = accounts.stream()
            .filter(a -> "Equity".equalsIgnoreCase(a.getAccountCategory()) &&
                         ("Retained Earnings".equalsIgnoreCase(a.getAccountSubcategory()) ||
                          a.getAccountName().equalsIgnoreCase("Retained Earnings")))
            .findFirst().orElse(null);

        // Calculate Retained Earnings based on CoA Static Balances
        BigDecimal retainedEarningsBalance = BigDecimal.ZERO;
        if(retainedEarningsAccount != null) {
             LocalDate startOfYear = asOfDate.withDayOfYear(1);
             RetainedEarningsReport reReport = buildRetainedEarningsInternal(startOfYear, asOfDate);
             retainedEarningsBalance = reReport.getEndingBalance();
             log.debug("Using Ending Retained Earnings from internal calculation (static): {}", retainedEarningsBalance);
        } else {
            log.warn("Retained Earnings account not found in filtered list!");
        }
        String reStyle = "account indent-2 underline";
        rows.add(new BalanceSheetRow("Retained Earnings", retainedEarningsBalance, 2, reStyle, firstEquityItem));


        // --- Total Equity ---
        BigDecimal totalEquity = contributedCapitalTotal.add(retainedEarningsBalance);
        rows.add(new BalanceSheetRow("Total Stockholders' Equity", totalEquity, 2, "total indent-1 bold underline", true));

        // --- Total Liabilities & Equity ---
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        rows.add(new BalanceSheetRow("Total Liabilities & Stockholders' Equity", totalLiabilitiesAndEquity, 2, "total bold double-underline", true));

        // --- Verification (REMOVED THE ERROR ROW ADDITION) ---
        if (totalAssets.compareTo(totalLiabilitiesAndEquity) != 0) {
             BigDecimal difference = totalAssets.subtract(totalLiabilitiesAndEquity).setScale(2, BigDecimal.ROUND_HALF_UP);
             // REMOVED: rows.add(new BalanceSheetRow("!!! BALANCE CHECK FAILED - Difference:", difference , 2, "error bold", true));
             log.error("BALANCE SHEET (using static CoA) DOES NOT BALANCE! Date Label={}, Assets={}, Liab+Equity={}, Diff={}", asOfDate, totalAssets, totalLiabilitiesAndEquity, difference);
        } else {
             log.info("Balance Sheet (using static CoA) BALANCES. Date Label={}, Total={}", asOfDate, totalAssets);
        }

        saveSnapshot("BS", asOfDate, asOfDate, rows);
        return rows;
    }

    private int getAssetSubCategoryOrder(String subCategory) {
        if (subCategory == null) return 99;
        switch (subCategory.toLowerCase()) {
            case "current asset": return 1;
            case "property plant & equipment": return 2;
            case "accumulated depreciation": return 3;
            default: return 99;
        }
    }

    private int getLiabilitySubCategoryOrder(String subCategory) {
         if (subCategory == null) return 99;
         switch (subCategory.toLowerCase()) {
             case "current liability": return 1;
             case "unearned revenue": return 2;
             default: return 99;
         }
     }

    private int getEquitySubCategoryOrder(String subCategory) {
         if (subCategory == null) return 99;
         switch (subCategory.toLowerCase()) {
             case "contributed capital": return 1;
             case "common stock": return 2;
             case "retained earnings": return 3;
             default: return 99;
         }
     }


    // --- Retained Earnings (Uses Static CoA Balance - NON-STANDARD) ---
     public RetainedEarningsReport buildRetainedEarnings(LocalDate start, LocalDate end) {
         log.warn("Building Retained Earnings statement using STATIC balances from Chart of Accounts. This is NON-STANDARD and does NOT reflect activity for the period {} to {}.", start, end);
         RetainedEarningsReport report = buildRetainedEarningsInternal(start, end);
         saveSnapshot("RE", start, end, report);
         return report;
     }

    private RetainedEarningsReport buildRetainedEarningsInternal(LocalDate start, LocalDate end) {
        BigDecimal netIncome = buildIncomeStatementInternal(start, end)
            .stream()
            .filter(r -> "Net Income".equals(r.getLabel()))
            .findFirst()
            .map(IncomeStatementRow::getAmount)
            .orElse(BigDecimal.ZERO);

        Account reAccount = accountRepo.findByAccountCategory("Equity", null).stream()
            .filter(a -> "Retained Earnings".equalsIgnoreCase(a.getAccountSubcategory()) || "Retained Earnings".equalsIgnoreCase(a.getAccountName()))
            .findFirst()
            .orElse(null);

        BigDecimal beginningBalance = BigDecimal.ZERO;
        if (reAccount != null) {
            beginningBalance = reAccount.getInitialBalance() != null ? reAccount.getInitialBalance() : BigDecimal.ZERO;
             log.debug("Using Initial Balance for RE Account {} as Beginning RE: {}", reAccount.getAccountNumber(), beginningBalance);
        } else {
             log.warn("Retained Earnings account not found for beginning balance lookup.");
        }

        BigDecimal dividends = accountRepo.findByAccountCategory("Equity", Sort.by("accountNumber")).stream()
             .filter(a -> a.getAccountName().toLowerCase().contains("dividend"))
             .map(a -> a.getBalance() != null ? a.getBalance().abs() : BigDecimal.ZERO)
             .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("Using Static CoA Balances for Dividends. Total Dividends Balance (abs): {}", dividends);

        BigDecimal endingBalance = beginningBalance.add(netIncome).subtract(dividends);

        log.debug("Static RE Calculation: StartLabel={}, EndLabel={}, BeginRE (Initial)={}, NI (Static)={}, Div (Static)={}, EndRE={}",
                  start, end, beginningBalance, netIncome, dividends, endingBalance);

        return new RetainedEarningsReport(beginningBalance, netIncome, dividends, endingBalance, start, end);
    }

    // --- Save Snapshot ---
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

     // --- Helper method in Controller or Service to load from snapshot ---
     public List<BalanceSheetRow> loadBalanceSheetFromSnapshot(Long snapshotId) throws Exception {
         ReportSnapshot ss = snapRepo.findById(snapshotId)
             .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));
         if (!"BS".equals(ss.getReportType())) {
             throw new IllegalArgumentException("Snapshot is not a Balance Sheet");
         }
         return objectMapper.readValue(ss.getPayloadJson(), new TypeReference<List<BalanceSheetRow>>() {});
     }
 }