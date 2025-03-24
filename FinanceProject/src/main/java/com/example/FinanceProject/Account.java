package com.example.FinanceProject;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_name", columnNames = {"account_name"}),
        @UniqueConstraint(name = "uk_account_number", columnNames = {"account_number"})
})
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "account_name", nullable = false)
    private String accountName;

    @NotNull
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    // c. Account description (optional)
    @Column(name = "account_description")
    private String accountDescription;

    // d. Normal side (optional)
    @Column(name = "normal_side")
    private String normalSide;

    // e. Account category (optional)
    @Column(name = "account_category")
    private String accountCategory;

    // f. Account subcategory (optional)
    @Column(name = "account_subcategory")
    private String accountSubcategory;

    // g. Initial balance (with two decimal precision)
    @Column(name = "initial_balance", precision = 15, scale = 2)
    private BigDecimal initialBalance = BigDecimal.ZERO;

    // h. Debit (with two decimal precision)
    @Column(name = "debit", precision = 15, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    // i. Credit (with two decimal precision)
    @Column(name = "credit", precision = 15, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    // j. Balance (with two decimal precision)
    @Column(name = "balance", precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    // k. Date/time account added
    @Column(name = "date_added")
    private LocalDateTime dateAdded = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // m. Order (optional; use Integer for numeric order)
    @Column(name = "account_order")
    private Integer accountOrder;

    // n. Statement (optional)
    @Column(name = "statement")
    private String statement;

    // o. Comment (optional; NVARCHAR(MAX))
    @Column(name = "comment")
    private String comment;

    // Active flag; true for active, false for deactivated
    @Column(name = "active")
    private boolean active = true;

    /**
     * Gets the ID of the associated user.
     *
     * @return The user ID or null if no user is associated
     */
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    /**
     * Sets the user by ID.
     * This method creates a proxy User object with only the ID set.
     * Useful for setting relationships without loading the full User entity.
     *
     * @param userId The ID of the user to associate with this account
     */
    public void setUserId(Long userId) {
        if (userId != null) {
            User userRef = new User();
            userRef.setId(userId);
            this.user = userRef;
        } else {
            this.user = null;
        }
    }
}
