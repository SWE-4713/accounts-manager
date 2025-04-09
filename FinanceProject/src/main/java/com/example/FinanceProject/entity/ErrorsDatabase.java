package com.example.FinanceProject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Errors")
public class ErrorsDatabase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`error id`")
    private Long errorId;

    @Column(name = "`error description`")
    private String errorDescription;

    public Long getErrorId() {
        return errorId;
    }

    public void setErrorId(Long errorId) {
        this.errorId = errorId;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}

