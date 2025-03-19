package com.example.FinanceProject;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"accountNumber"}),
           @UniqueConstraint(columnNames = {"accountName"})
       })
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // a. Account name (unique)
    @NotNull
    @Column(name = "accountName", nullable = false)
    private String accountName;

    // b. Account number (only digits, no decimals, unique)
    @NotNull
    @Pattern(regexp="\\d+", message="Account number must be numeric with no decimals")
    @Column(name = "accountNumber", nullable = false)
    private String accountNumber;

    // c. Account description
    private String accountDescription;

    // d. Normal side (e.g., debit or credit side)
    private String normalSide;

    // e. Account category (e.g., asset)
    private String accountCategory;

    // f. Account subcategory (e.g., current assets)
    private String accountSubcategory;

    // g. Initial balance
    @Digits(integer = 15, fraction = 2)
    private BigDecimal initialBalance;

    // h. Debit
    @Digits(integer = 15, fraction = 2)
    private BigDecimal debit;

    // i. Credit
    @Digits(integer = 15, fraction = 2)
    private BigDecimal credit;

    // j. Balance
    @Digits(integer = 15, fraction = 2)
    private BigDecimal balance;

    // k. Date/time account added
    private LocalDateTime dateAdded;

    // l. User id (creator/owner)
    private Long userId;

    // m. Order (e.g., "01" for cash)
    private String accountOrder;

    // n. Statement (e.g., "IS", "BS", "RE")
    private String statement;

    // o. Comment
    private String comment;

    // Flag to indicate if the account is active or deactivated
    private boolean active = true;
}
