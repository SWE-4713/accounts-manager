package com.example.FinanceProject.repository;

import com.example.FinanceProject.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Sort;

public interface AccountRepo extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByAccountName(String accountName);

    // New method for searching by account number or name (can be extended to other fields)
    List<Account> findByAccountNumberContainingOrAccountNameContaining(String accountNumber, String accountName, Sort sort);
    List<Account> findByAccountCategory(String accountCategory, Sort sort);
}
