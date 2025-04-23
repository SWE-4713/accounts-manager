package com.example.FinanceProject.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.entity.User;

public interface AccountRepo extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByAccountName(String accountName);
    Optional<Account> findTopByAccountNumberStartingWithOrderByAccountNumberDesc(String prefix);
    List<Account> findByUser(User user);

    // New method for searching by account number or name (can be extended to other fields)
    List<Account> findByAccountNumberContainingOrAccountNameContaining(String accountNumber, String accountName, Sort sort);
    List<Account> findByAccountCategory(String accountCategory, Sort sort);
    List<Account> findByAccountCategoryIn(List<String> categories);

    List<Account> findByStatement(String statement);
}
