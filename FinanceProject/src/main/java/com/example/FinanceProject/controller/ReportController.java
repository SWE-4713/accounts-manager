package com.example.FinanceProject.controller;

import com.example.FinanceProject.dto.*;
import com.example.FinanceProject.entity.ReportSnapshot;
import com.example.FinanceProject.repository.ReportSnapshotRepo;
import com.example.FinanceProject.service.FinancialReportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map; // Import Map
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final FinancialReportService reportService;
    private final ReportSnapshotRepo snapRepo;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReportController(FinancialReportService reportService,
                            ReportSnapshotRepo snapRepo,
                            ObjectMapper objectMapper) {
        this.reportService = reportService;
        this.snapRepo       = snapRepo;
        this.objectMapper   = objectMapper;
    }

    private List<ReportSnapshot> loadSnapshots(String type) {
        return snapRepo.findByReportTypeOrderByGeneratedAtDesc(type);
    }

    private String formatButtonLabel(String abbr, LocalDate start, LocalDate end) {
        if ("BS".equals(abbr)) {
            return abbr + " " + start.toString();
        } else if (start.equals(end)) { // Handle single date case for other reports
             return abbr + " " + start.toString();
         }else {
            return abbr + " " + start.toString() + "-" + end.toString();
        }
    }

    // --- Common Logic to Prepare Model Attributes ---
    private void prepareModelForReport(Model model, String reportAbbr, Long snapshotId, LocalDate startDate, LocalDate endDate, Authentication authentication) {
         List<ReportSnapshot> tabs = loadSnapshots(reportAbbr);
         List<Map<String, String>> snapshotButtons = tabs.stream().map(s -> Map.of(
            "id", s.getId().toString(),
            "text", formatButtonLabel(reportAbbr, s.getStartDate(), s.getEndDate())
        )).collect(Collectors.toList());

        model.addAttribute("snapshots",   snapshotButtons);
        model.addAttribute("snapshotId",  snapshotId);
        model.addAttribute("abbr",        reportAbbr);
        model.addAttribute("startDate",   startDate); // Pass dates even if report not generated yet
        model.addAttribute("endDate",     endDate);   // Pass dates even if report not generated yet
        model.addAttribute("date",        startDate); // Pass single date for BS consistency
        model.addAttribute("username",    authentication.getName());
    }


    // ── Trial Balance ───────────────────────────────────────────────────
    @GetMapping("/trial-balance")
    public String trialBalance(
        @RequestParam(value="startDate", required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(value="endDate",   required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
        @RequestParam(value="snapshotId", required=false) Long snapshotId,
        Model model, Authentication authentication) throws Exception {

        List<TrialBalanceRow> rows = null; // Initialize as null
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        // Logic to load/generate report *only* if dates or snapshotId provided
        if (snapshotId != null) {
            ReportSnapshot ss = snapRepo.findById(snapshotId).orElseThrow();
            rows = objectMapper.readValue(
                    ss.getPayloadJson(),
                    new TypeReference<List<TrialBalanceRow>>() {});
            start = ss.getStartDate();
            end   = ss.getEndDate();
        } else if (start != null) { // Generate only if start date is present
             if (end == null) end = start; // Handle single date case
            rows = reportService.buildTrialBalance(start, end); // Generates and saves snapshot
        }
        // If rows were loaded or generated, calculate totals
        if (rows != null) {
             debitTotal = rows.stream()
                .map(TrialBalanceRow::getDebit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
             creditTotal = rows.stream()
                .map(TrialBalanceRow::getCredit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            rows = List.of(); // Ensure rows is an empty list if not generated/loaded
        }

        prepareModelForReport(model, "TB", snapshotId, start, end, authentication);
        model.addAttribute("rows",        rows); // Pass potentially empty list or null
        model.addAttribute("debitTotal",  debitTotal);
        model.addAttribute("creditTotal", creditTotal);

        return "trial-balance";
    }

    // ── Income Statement ───────────────────────────────────────────────
    @GetMapping("/income-statement")
    public String incomeStatement(
        @RequestParam(value="startDate", required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(value="endDate",   required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
        @RequestParam(value="snapshotId", required=false) Long snapshotId,
        Model model, Authentication authentication) throws Exception {

        List<IncomeStatementRow> rows = null; // Initialize as null
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal netIncome = BigDecimal.ZERO;

         // Logic to load/generate report *only* if dates or snapshotId provided
        if (snapshotId != null) {
            ReportSnapshot ss = snapRepo.findById(snapshotId).orElseThrow();
            rows = objectMapper.readValue(
                    ss.getPayloadJson(),
                    new TypeReference<List<IncomeStatementRow>>() {});
            start = ss.getStartDate();
            end   = ss.getEndDate();
        } else if (start != null) { // Generate only if start date is present
             if (end == null) end = start; // Handle single date case
            rows = reportService.buildIncomeStatement(start, end); // Generates and saves snapshot
        }

        // If rows were loaded or generated, calculate totals
        if (rows != null) {
            totalRevenue = rows.stream()
                .filter(r -> "Total Revenue".equals(r.getLabel()) && r.getAmount() != null)
                .map(IncomeStatementRow::getAmount)
                .findFirst().orElse(BigDecimal.ZERO);
             totalExpenses = rows.stream()
                .filter(r -> "Total Expenses".equals(r.getLabel()) && r.getAmount() != null)
                .map(IncomeStatementRow::getAmount)
                .findFirst().orElse(BigDecimal.ZERO);
             netIncome = rows.stream()
                .filter(r -> "Net Income".equals(r.getLabel()) && r.getAmount() != null)
                .map(IncomeStatementRow::getAmount)
                .findFirst().orElse(BigDecimal.ZERO);
        } else {
             rows = List.of(); // Ensure rows is an empty list if not generated/loaded
        }


        prepareModelForReport(model, "IS", snapshotId, start, end, authentication);
        model.addAttribute("rows", rows); // Pass potentially empty list or null
        model.addAttribute("totalRevenue",  totalRevenue);
        model.addAttribute("totalExpenses",  totalExpenses);
        model.addAttribute("netIncome",     netIncome);

        return "income-statement";
    }

    // ── Balance Sheet ─────────────────────────────────────────────────
    @GetMapping("/balance-sheet")
    public String balanceSheet(
        @RequestParam(value="date", required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(value="snapshotId", required=false) Long snapshotId,
        Model model, Authentication authentication) throws Exception {

        List<BalanceSheetRow> rows = null;
        BigDecimal finalTotalAssets = BigDecimal.ZERO; // Added for direct access in template if needed
        BigDecimal finalTotalLiabilitiesAndEquity = BigDecimal.ZERO; // Added for direct access

        if (snapshotId != null) {
            ReportSnapshot ss = snapRepo.findById(snapshotId).orElseThrow();
            rows = objectMapper.readValue(
                    ss.getPayloadJson(),
                    new TypeReference<List<BalanceSheetRow>>() {});
            date = ss.getStartDate();
        } else if (date != null) {
            rows = reportService.buildBalanceSheet(date);
        }

        if (rows != null) {
            // Extract final totals from the generated rows for easy access in the template
            finalTotalAssets = rows.stream()
                .filter(r -> "Total Assets".equals(r.getLabel()))
                .map(BalanceSheetRow::getAmount)
                .findFirst().orElse(BigDecimal.ZERO);
            finalTotalLiabilitiesAndEquity = rows.stream()
                .filter(r -> "Total Liabilities & Stockholders' Equity".equals(r.getLabel()))
                .map(BalanceSheetRow::getAmount)
                .findFirst().orElse(BigDecimal.ZERO);
        } else {
             rows = List.of();
        }

        prepareModelForReport(model, "BS", snapshotId, date, date, authentication);
        model.addAttribute("rows", rows);
        model.addAttribute("finalTotalAssets", finalTotalAssets); // Pass final total
        model.addAttribute("finalTotalLiabilitiesAndEquity", finalTotalLiabilitiesAndEquity); // Pass final total

        return "balance-sheet";
    }

    // ── Retained Earnings ───────────────────────────────────────────────
    @GetMapping("/retained-earning")
    public String retainedEarnings(
        @RequestParam(value="startDate",    required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(value="endDate",      required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
        @RequestParam(value="snapshotId",   required=false) Long snapshotId,
        Model model, Authentication authentication) throws Exception {

        RetainedEarningsReport report = null; // Initialize as null

         // Logic to load/generate report *only* if dates or snapshotId provided
        if (snapshotId != null) {
            ReportSnapshot ss = snapRepo.findById(snapshotId).orElseThrow();
            report = objectMapper.readValue(
                ss.getPayloadJson(),
                RetainedEarningsReport.class
            );
            start = ss.getStartDate();
            end   = ss.getEndDate();
        } else if (start != null) { // Generate only if start date is present
             if (end == null) end = start; // Handle single date case
            report = reportService.buildRetainedEarnings(start, end); // Generates and saves snapshot
        }

        prepareModelForReport(model, "RE", snapshotId, start, end, authentication);
        model.addAttribute("report",        report); // Pass potentially null report

        return "retained-earning";
    }
}