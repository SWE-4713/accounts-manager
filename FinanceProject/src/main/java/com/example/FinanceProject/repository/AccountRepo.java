package com.example.FinanceProject.repository;

import com.example.FinanceProject.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AccountRepo extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByAccountName(String accountName);

    // New method for searching by account number or name (can be extended to other fields)
    List<Account> findByAccountNumberContainingOrAccountNameContaining(String accountNumber, String accountName);
}
