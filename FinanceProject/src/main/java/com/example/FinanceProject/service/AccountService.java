// AccountService.java
package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.FinanceProject.entity.User;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Service
public class AccountService {

    @Autowired
    private AccountRepo accountRepo;
    
    @Autowired
    private EventLogService eventLogService; // Inject our event log service

    private UserService userService;

    public Account addAccount(Account account) {
        // Generate account number (as previously defined)
        String generatedNumber = generateAccountNumber(account);
        account.setAccountNumber(generatedNumber);

        // Set the date/time account was added
        account.setDateAdded(LocalDateTime.now());

        // Ensure monetary values have two decimals
        if (account.getInitialBalance() != null) {
            account.setInitialBalance(account.getInitialBalance().setScale(2, RoundingMode.HALF_UP));
        }
        if (account.getDebit() != null) {
            account.setDebit(account.getDebit().setScale(2, RoundingMode.HALF_UP));
        }
        if (account.getCredit() != null) {
            account.setCredit(account.getCredit().setScale(2, RoundingMode.HALF_UP));
        }
        if (account.getBalance() != null) {
            account.setBalance(account.getBalance().setScale(2, RoundingMode.HALF_UP));
        }

        // Set the current user as the creator of the account
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            User currentUser = userService.findUserByUsername(auth.getName());
            account.setUser(currentUser);
        }

        Account saved = accountRepo.save(account);
        eventLogService.logEvent(null, saved, saved.getUserId(), "ADD");
        return saved;
    }

    private String generateAccountNumber(Account account) {
        // Determine the first digit based on account category (adjust these as needed)
        String category = account.getAccountCategory();
        char firstDigit;
        switch (category.toLowerCase()) {
            case "asset":
                firstDigit = '1';
                break;
            case "liability":
                firstDigit = '2';
                break;
            case "equity":
                firstDigit = '3';
                break;
            case "revenue":
                firstDigit = '4';
                break;
            case "expense":
                firstDigit = '5';
                break;
            default:
                firstDigit = '6';
                break;
        }
        String prefix = String.valueOf(firstDigit);

        // Query for the last account with this prefix
        Optional<Account> lastAccountOpt = accountRepo.findTopByAccountNumberStartingWithOrderByAccountNumberDesc(prefix);
        long newNumber;
        if (lastAccountOpt.isPresent()) {
            String lastNumberStr = lastAccountOpt.get().getAccountNumber();
            long lastNumber = Long.parseLong(lastNumberStr);
            newNumber = lastNumber + 10;
        } else {
            // Starting value when none exists (for example, for assets: 1000000010)
            newNumber = Long.parseLong(prefix + "000000010");
        }
        // Format as a 10-digit string (will pad with zeros if needed)
        return String.format("%010d", newNumber);
    }
    
    public List<Account> getAllAccounts(Sort sort) {
        return accountRepo.findAll(sort);
    }
    
    public Account getAccountById(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }
    
    public Account updateAccount(Account updatedAccount) {
        Account existing = getAccountById(updatedAccount.getId());
        
        // Check if account number/name is changed to one that already exists.
        if (!existing.getAccountNumber().equals(updatedAccount.getAccountNumber())) {
            if (accountRepo.findByAccountNumber(updatedAccount.getAccountNumber()).isPresent()) {
                throw new IllegalArgumentException("Account number already exists.");
            }
        }
        if (!existing.getAccountName().equals(updatedAccount.getAccountName())) {
            if (accountRepo.findByAccountName(updatedAccount.getAccountName()).isPresent()) {
                throw new IllegalArgumentException("Account name already exists.");
            }
        }
        
        // Capture before-image for event log
        Account beforeUpdate = new Account();
        beforeUpdate.setId(existing.getId());
        beforeUpdate.setAccountName(existing.getAccountName());
        beforeUpdate.setAccountNumber(existing.getAccountNumber());
        beforeUpdate.setBalance(existing.getBalance());
        // (Copy other fields as needed)

        // Update fields (other fields such as dateAdded remain unchanged)
        existing.setAccountName(updatedAccount.getAccountName());
        existing.setAccountNumber(updatedAccount.getAccountNumber());
        existing.setAccountDescription(updatedAccount.getAccountDescription());
        existing.setNormalSide(updatedAccount.getNormalSide());
        existing.setAccountCategory(updatedAccount.getAccountCategory());
        existing.setAccountSubcategory(updatedAccount.getAccountSubcategory());
        existing.setInitialBalance(updatedAccount.getInitialBalance().setScale(2, RoundingMode.HALF_UP));
        existing.setDebit(updatedAccount.getDebit().setScale(2, RoundingMode.HALF_UP));
        existing.setCredit(updatedAccount.getCredit().setScale(2, RoundingMode.HALF_UP));
        existing.setBalance(updatedAccount.getBalance().setScale(2, RoundingMode.HALF_UP));
        existing.setUserId(updatedAccount.getUserId());
        existing.setAccountOrder(updatedAccount.getAccountOrder());
        existing.setStatement(updatedAccount.getStatement());
        existing.setComment(updatedAccount.getComment());
        
        Account saved = accountRepo.save(existing);
        // Log event: record before and after images.
        eventLogService.logEvent(beforeUpdate, saved, saved.getUserId(), "MODIFY");
        return saved;
    }
    
    public void deactivateAccount(Long id) {
        Account account = getAccountById(id);
        // 6. Check that balance is zero before deactivation
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Accounts with balance greater than zero cannot be deactivated.");
        }
        
        // Capture before-image for event log
        Account beforeDeactivation = new Account();
        beforeDeactivation.setId(account.getId());
        beforeDeactivation.setActive(account.isActive());
        // (Copy additional fields if needed)
        
        account.setActive(false);
        Account saved = accountRepo.save(account);
        
        // Log event: record deactivation event
        eventLogService.logEvent(beforeDeactivation, saved, saved.getUserId(), "DEACTIVATE");
    }
    
    // 8 & 12. Search/filter accounts by various tokens (account name, number, category, etc.)
    public List<Account> searchAccounts(String query, Sort sort) {
        // For simplicity, we use a contains search on both account number and name.
        return accountRepo.findByAccountNumberContainingOrAccountNameContaining(query, query, sort);
    }
    
    public List<Account> filterAccountsByCategory(String category, Sort sort) {
        return accountRepo.findByAccountCategory(category, sort);
    }
    
    public List<Account> searchAndFilterAccounts(String query, String category, Sort sort) {
        // Combine search and filter – here we perform search then filter in memory.
        List<Account> searched = searchAccounts(query, sort);
        return searched.stream()
                       .filter(a -> a.getAccountCategory().equalsIgnoreCase(category))
                       .toList();
    }
}
