// src/main/java/com/example/FinanceProject/controller/ReportController.java
package com.example.FinanceProject.controller;

// --- Keep existing imports ---
import com.example.FinanceProject.dto.*;
import com.example.FinanceProject.entity.ReportSnapshot;
import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.repository.ReportSnapshotRepo;
import com.example.FinanceProject.repository.UserRepo;
import com.example.FinanceProject.service.EmailService;
import com.example.FinanceProject.service.FinancialReportService;
import com.example.FinanceProject.service.PDFReportService; // Import the new service
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException; // Import DocumentException

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files; // For temporary file handling
import java.nio.file.Path; // For temporary file handling
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
// Removed text formatting helpers (pad*, formatAmount*, etc.)

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final FinancialReportService reportService;
    private final ReportSnapshotRepo snapRepo;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final PDFReportService pdfReportService; // Inject PdfReportService

    @Autowired
    public ReportController(FinancialReportService reportService,
                            ReportSnapshotRepo snapRepo,
                            ObjectMapper objectMapper,
                            EmailService emailService,
                            PDFReportService pdfReportService) { // Add PdfReportService
        this.reportService = reportService;
        this.snapRepo       = snapRepo;
        this.objectMapper   = objectMapper;
        this.emailService = emailService;
        this.pdfReportService = pdfReportService; // Initialize
    }

    @Autowired
    private UserRepo userRepo;

    // --- Remove convertReportToFixedWidthText and related helpers ---

    // --- Keep existing report mapping methods (trialBalance, incomeStatement, etc.) ---
    // --- They remain unchanged as they only prepare data for the web view ---
     @GetMapping("/trial-balance")
    public String trialBalance(
        @RequestParam(value="startDate", required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(value="endDate",   required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
        @RequestParam(value="snapshotId", required=false) Long snapshotId,
        Model model, Authentication authentication) throws Exception {

        List<User> managers = userRepo.findByRoleAndStatusNot("ROLE_MANAGER", "INACTIVE"); // or whatever filter you want
        List<User> accountants = userRepo.findByRoleAndStatusNot("ROLE_USER", "INACTIVE"); // or whatever filter you want
        Set<String> emailSet = new HashSet<>();
        emailSet.addAll(managers.stream().map(User::getEmail).toList());
        emailSet.addAll(accountants.stream().map(User::getEmail).toList());
        List<String> emails = new ArrayList<>(emailSet);
        model.addAttribute("userEmails", emails);

        List<TrialBalanceRow> rows = null; // Initialize as null
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        if (snapshotId != null) {
            ReportSnapshot ss = snapRepo.findById(snapshotId).orElseThrow(() -> new RuntimeException("Snapshot not found"));
            rows = objectMapper.readValue(ss.getPayloadJson(), new TypeReference<List<TrialBalanceRow>>() {});
            start = ss.getStartDate();
            end   = ss.getEndDate();
        } else if (start != null) {
             if (end == null) end = start;
            rows = reportService.buildTrialBalance(start, end);
        }

        if (rows != null) {
             debitTotal = rows.stream().map(TrialBalanceRow::getDebit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
             creditTotal = rows.stream().map(TrialBalanceRow::getCredit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            rows = List.of();
        }

        prepareModelForReport(model, "TB", snapshotId, start, end, authentication);
        model.addAttribute("rows", rows);
        model.addAttribute("debitTotal",  debitTotal);
        model.addAttribute("creditTotal", creditTotal);

        return "trial-balance";
    }

    @GetMapping("/income-statement")
    public String incomeStatement(
        @RequestParam(value="startDate", required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(value="endDate",   required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
        @RequestParam(value="snapshotId", required=false) Long snapshotId,
        Model model, Authentication authentication) throws Exception {

        List<User> managers = userRepo.findByRoleAndStatusNot("ROLE_MANAGER", "INACTIVE"); // or whatever filter you want
        List<User> accountants = userRepo.findByRoleAndStatusNot("ROLE_USER", "INACTIVE"); // or whatever filter you want
        Set<String> emailSet = new HashSet<>();
        emailSet.addAll(managers.stream().map(User::getEmail).toList());
        emailSet.addAll(accountants.stream().map(User::getEmail).toList());
        List<String> emails = new ArrayList<>(emailSet);
        model.addAttribute("userEmails", emails);

        List<IncomeStatementRow> rows = null;

         if (snapshotId != null) {
             ReportSnapshot ss = snapRepo.findById(snapshotId).orElseThrow(() -> new RuntimeException("Snapshot not found"));
             rows = objectMapper.readValue(ss.getPayloadJson(), new TypeReference<List<IncomeStatementRow>>() {});
             start = ss.getStartDate();
             end   = ss.getEndDate();
         } else if (start != null) {
              if (end == null) end = start;
             rows = reportService.buildIncomeStatement(start, end);
         }

         if (rows == null) {
              rows = List.of();
         }


        prepareModelForReport(model, "IS", snapshotId, start, end, authentication);
        model.addAttribute("rows", rows);
         BigDecimal netIncome = rows.stream()
            .filter(r -> "Net Income".equals(r.getLabel()) && r.getAmount() != null)
            .map(IncomeStatementRow::getAmount)
            .findFirst().orElse(BigDecimal.ZERO);
         model.addAttribute("netIncome", netIncome);


        return "income-statement";
    }

    @GetMapping("/balance-sheet")
    public String balanceSheet(
        @RequestParam(value="date", required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(value="snapshotId", required=false) Long snapshotId,
        Model model, Authentication authentication) throws Exception {

        List<User> managers = userRepo.findByRoleAndStatusNot("ROLE_MANAGER", "INACTIVE"); // or whatever filter you want
        List<User> accountants = userRepo.findByRoleAndStatusNot("ROLE_USER", "INACTIVE"); // or whatever filter you want
        Set<String> emailSet = new HashSet<>();
        emailSet.addAll(managers.stream().map(User::getEmail).toList());
        emailSet.addAll(accountants.stream().map(User::getEmail).toList());
        List<String> emails = new ArrayList<>(emailSet);
        model.addAttribute("userEmails", emails);

        List<BalanceSheetRow> rows = null;

        if (snapshotId != null) {
            ReportSnapshot ss = snapRepo.findById(snapshotId).orElseThrow(() -> new RuntimeException("Snapshot not found"));
            rows = objectMapper.readValue(ss.getPayloadJson(), new TypeReference<List<BalanceSheetRow>>() {});
            date = ss.getStartDate();
        } else if (date != null) {
            rows = reportService.buildBalanceSheet(date);
        }

        if (rows == null) {
             rows = List.of();
        }

        prepareModelForReport(model, "BS", snapshotId, date, date, authentication);
        model.addAttribute("rows", rows);
        model.addAttribute("finalTotalAssets", rows.stream()
            .filter(r -> "Total Assets".equals(r.getLabel()) && r.getAmount() != null)
            .map(BalanceSheetRow::getAmount)
            .findFirst().orElse(BigDecimal.ZERO));
        model.addAttribute("finalTotalLiabilitiesAndEquity", rows.stream()
            .filter(r -> "Total Liabilities & Stockholders' Equity".equals(r.getLabel()) && r.getAmount() != null)
            .map(BalanceSheetRow::getAmount)
            .findFirst().orElse(BigDecimal.ZERO));


        return "balance-sheet";
    }

    @GetMapping("/retained-earning")
    public String retainedEarnings(
        @RequestParam(value="startDate",    required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(value="endDate",      required=false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
        @RequestParam(value="snapshotId",   required=false) Long snapshotId,
        Model model, Authentication authentication) throws Exception {

        List<User> managers = userRepo.findByRoleAndStatusNot("ROLE_MANAGER", "INACTIVE"); // or whatever filter you want
        List<User> accountants = userRepo.findByRoleAndStatusNot("ROLE_USER", "INACTIVE"); // or whatever filter you want
        Set<String> emailSet = new HashSet<>();
        emailSet.addAll(managers.stream().map(User::getEmail).toList());
        emailSet.addAll(accountants.stream().map(User::getEmail).toList());
        List<String> emails = new ArrayList<>(emailSet);
        model.addAttribute("userEmails", emails);

        RetainedEarningsReport report = null;

        if (snapshotId != null) {
            ReportSnapshot ss = snapRepo.findById(snapshotId).orElseThrow(() -> new RuntimeException("Snapshot not found"));
            report = objectMapper.readValue(ss.getPayloadJson(), RetainedEarningsReport.class);
            start = ss.getStartDate();
            end   = ss.getEndDate();
        } else if (start != null) {
             if (end == null) end = start;
            report = reportService.buildRetainedEarnings(start, end);
        }

        prepareModelForReport(model, "RE", snapshotId, start, end, authentication);
        model.addAttribute("report", report);

        return "retained-earning";
    }

    // --- Common Model Prep & Snapshot loading ---
    private void prepareModelForReport(Model model, String reportAbbr, Long snapshotId, LocalDate startDate, LocalDate endDate, Authentication authentication) {
         List<ReportSnapshot> tabs = loadSnapshots(reportAbbr);
         List<Map<String, String>> snapshotButtons = tabs.stream()
             .filter(s -> s.getReportType().equals(reportAbbr))
             .map(s -> Map.of(
                 "id", s.getId().toString(),
                 "text", formatButtonLabel(s.getReportType(), s.getStartDate(), s.getEndDate())
             )).collect(Collectors.toList());

        model.addAttribute("snapshots",   snapshotButtons);
        model.addAttribute("snapshotId",  snapshotId);
        model.addAttribute("abbr",        reportAbbr);
        model.addAttribute("startDate",   startDate);
        model.addAttribute("endDate",     endDate);
        model.addAttribute("date",        startDate);
        model.addAttribute("username",    authentication != null ? authentication.getName() : "System");

        if (snapshotId != null && !tabs.isEmpty()) {
             ReportSnapshot currentSnapshot = tabs.stream().filter(s -> s.getId().equals(snapshotId)).findFirst().orElse(null);
             if (currentSnapshot != null) {
                  String dateLabel = "BS".equals(currentSnapshot.getReportType()) ?
                      "As of " + currentSnapshot.getStartDate().format(DateTimeFormatter.ISO_DATE) :
                      "For the period " + currentSnapshot.getStartDate().format(DateTimeFormatter.ISO_DATE) + " to " + currentSnapshot.getEndDate().format(DateTimeFormatter.ISO_DATE);
                 model.addAttribute("snapshotDateLabel", dateLabel);
             }
         }
    }
     private List<ReportSnapshot> loadSnapshots(String type) {
        return snapRepo.findByReportTypeOrderByGeneratedAtDesc(type);
    }
    private String formatButtonLabel(String abbr, LocalDate start, LocalDate end) {
        DateTimeFormatter btnDateFormatter = DateTimeFormatter.ofPattern("MM-dd-yy"); // Shorter format for buttons
         if ("BS".equals(abbr)) {
             return abbr + " " + start.format(btnDateFormatter);
         } else {
             return abbr + " " + start.format(btnDateFormatter) + "_" + end.format(btnDateFormatter);
         }
     }

    // --- UPDATED ENDPOINTS FOR ACTIONS ---

    @GetMapping("/download/{snapshotId}")
    public ResponseEntity<Resource> downloadReport(
            @PathVariable Long snapshotId) { // reportType param no longer needed here

        try {
            ReportSnapshot snapshot = snapRepo.findById(snapshotId)
                    .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));

            byte[] pdfData;
            String reportType = snapshot.getReportType();

            // Generate PDF based on report type
            switch (reportType) {
                case "TB":
                    pdfData = pdfReportService.generateTrialBalancePdf(snapshot.getPayloadJson(), snapshot.getStartDate(), snapshot.getEndDate());
                    break;
                case "IS":
                    pdfData = pdfReportService.generateIncomeStatementPdf(snapshot.getPayloadJson(), snapshot.getStartDate(), snapshot.getEndDate());
                    break;
                case "BS":
                    pdfData = pdfReportService.generateBalanceSheetPdf(snapshot.getPayloadJson(), snapshot.getStartDate()); // BS only needs one date
                    break;
                case "RE":
                    pdfData = pdfReportService.generateRetainedEarningsPdf(snapshot.getPayloadJson(), snapshot.getStartDate(), snapshot.getEndDate());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported report type for PDF download: " + reportType);
            }

            ByteArrayResource resource = new ByteArrayResource(pdfData);

            // Generate filename with .pdf extension
            String filename = reportType + "_" +
                              snapshot.getStartDate().toString() +
                              (snapshot.getStartDate().equals(snapshot.getEndDate()) ? "" : "_" + snapshot.getEndDate().toString()) +
                              ".pdf"; // Changed extension to .pdf

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF) // Use application/pdf MIME type
                    .contentLength(pdfData.length)
                    .body(resource);

        } catch (IOException | DocumentException e) { // Added DocumentException
            System.err.println("Error generating PDF for download: " + e.getMessage());
            // Consider returning a user-friendly error response
            return ResponseEntity.status(500).body(null);
        } catch (RuntimeException e) {
             System.err.println("Download error: " + e.getMessage());
             return ResponseEntity.status(404).body(null); // Snapshot not found
        }
    }

     @PostMapping("/email/{snapshotId}")
     @ResponseBody
     public ResponseEntity<Map<String, Object>> emailReport(
             @PathVariable Long snapshotId,
             @RequestParam String reportName, // Keep using this for subject/body
             @RequestParam String email,
             Authentication authentication) {

         Map<String, Object> response = new java.util.HashMap<>();
         Path tempFilePath = null; // Define outside try for finally block
         String userEmail = email;

         try {
             ReportSnapshot snapshot = snapRepo.findById(snapshotId)
                     .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));

            byte[] pdfData;
            String reportType = snapshot.getReportType();

            // Generate PDF based on report type
            switch (reportType) {
                 case "TB":
                    pdfData = pdfReportService.generateTrialBalancePdf(snapshot.getPayloadJson(), snapshot.getStartDate(), snapshot.getEndDate());
                    break;
                case "IS":
                    pdfData = pdfReportService.generateIncomeStatementPdf(snapshot.getPayloadJson(), snapshot.getStartDate(), snapshot.getEndDate());
                    break;
                case "BS":
                    pdfData = pdfReportService.generateBalanceSheetPdf(snapshot.getPayloadJson(), snapshot.getStartDate());
                    break;
                case "RE":
                    pdfData = pdfReportService.generateRetainedEarningsPdf(snapshot.getPayloadJson(), snapshot.getStartDate(), snapshot.getEndDate());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported report type for PDF email: " + reportType);
            }

             String subject = "Financial Report: " + reportName;
             String body = "Please find the attached financial report (PDF): " + reportName;
             // Generate filename with .pdf extension
             String filename = reportName.replace(" ", "_").replace("/", "-") + ".pdf"; // Changed extension

             // Write PDF data to a temporary file to use with existing EmailService
             tempFilePath = Files.createTempFile("report_", ".pdf"); // Use .pdf extension
             Files.write(tempFilePath, pdfData);

             emailService.sendEmailWithAttachment(userEmail, subject, body, tempFilePath.toString(), filename);

             response.put("success", true);
             response.put("message", "Email sent successfully to " + userEmail);
             return ResponseEntity.ok(response);

         } catch (IOException | DocumentException | MessagingException e) { // Added DocumentException
             System.err.println("Error emailing PDF report: " + e.getMessage());
             response.put("success", false);
             response.put("message", "Failed to send email report.");
             return ResponseEntity.status(500).body(response);
         } catch (RuntimeException e) {
              System.err.println("Unexpected error emailing report: " + e.getMessage());
              response.put("success", false);
              response.put("message", "An error occurred while preparing the email: " + e.getMessage());
              return ResponseEntity.status(500).body(response);
        } finally {
             // Clean up temp file if it was created
             if (tempFilePath != null) {
                 try {
                     Files.deleteIfExists(tempFilePath);
                 } catch (IOException e) {
                     System.err.println("Failed to delete temporary email file: " + tempFilePath + " - " + e.getMessage());
                 }
             }
         }
     }


    // --- Keep the /print/{snapshotId} endpoint as is ---
    // It renders HTML for the browser's print dialog, not a PDF file.
    @GetMapping("/print/{snapshotId}")
    public String printReport(@PathVariable Long snapshotId, Model model) {
         try {
             ReportSnapshot snapshot = snapRepo.findById(snapshotId)
                 .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));
             String reportType = snapshot.getReportType();
             LocalDate startDate = snapshot.getStartDate();
             LocalDate endDate = snapshot.getEndDate();
             Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


             prepareModelForReport(model, reportType, snapshotId, startDate, endDate, authentication);
             model.addAttribute("isPrintView", true);

             if ("TB".equals(reportType)) {
                  List<TrialBalanceRow> rows = objectMapper.readValue(snapshot.getPayloadJson(), new TypeReference<List<TrialBalanceRow>>() {});
                  BigDecimal debitTotal = rows.stream().map(TrialBalanceRow::getDebit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                  BigDecimal creditTotal = rows.stream().map(TrialBalanceRow::getCredit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                  model.addAttribute("rows", rows);
                  model.addAttribute("debitTotal", debitTotal);
                  model.addAttribute("creditTotal", creditTotal);
                  return "trial-balance";
             } else if ("IS".equals(reportType)) {
                  List<IncomeStatementRow> rows = objectMapper.readValue(snapshot.getPayloadJson(), new TypeReference<List<IncomeStatementRow>>() {});
                  model.addAttribute("rows", rows);
                   BigDecimal netIncome = rows.stream()
                        .filter(r -> "Net Income".equals(r.getLabel()) && r.getAmount() != null)
                        .map(IncomeStatementRow::getAmount)
                        .findFirst().orElse(BigDecimal.ZERO);
                   model.addAttribute("netIncome", netIncome);
                  return "income-statement";
             } else if ("BS".equals(reportType)) {
                  List<BalanceSheetRow> rows = objectMapper.readValue(snapshot.getPayloadJson(), new TypeReference<List<BalanceSheetRow>>() {});
                  model.addAttribute("rows", rows);
                  model.addAttribute("finalTotalAssets", rows.stream()
                     .filter(r -> "Total Assets".equals(r.getLabel()) && r.getAmount() != null)
                     .map(BalanceSheetRow::getAmount)
                     .findFirst().orElse(BigDecimal.ZERO));
                 model.addAttribute("finalTotalLiabilitiesAndEquity", rows.stream()
                     .filter(r -> "Total Liabilities & Stockholders' Equity".equals(r.getLabel()) && r.getAmount() != null)
                     .map(BalanceSheetRow::getAmount)
                     .findFirst().orElse(BigDecimal.ZERO));
                  return "balance-sheet";
             } else if ("RE".equals(reportType)) {
                  RetainedEarningsReport report = objectMapper.readValue(snapshot.getPayloadJson(), RetainedEarningsReport.class);
                  model.addAttribute("report", report);
                  return "retained-earning";
             } else {
                 throw new IllegalArgumentException("Unsupported report type for printing: " + reportType);
             }

         } catch (IOException e) {
             System.err.println("Error generating print view: " + e.getMessage());
             model.addAttribute("error", "Failed to load report data for printing.");
             return "error-page";
          } catch (RuntimeException e) {
             System.err.println("Print view error: " + e.getMessage());
             model.addAttribute("error", "Could not find the requested report snapshot.");
             return "error-page";
         }
    }
}