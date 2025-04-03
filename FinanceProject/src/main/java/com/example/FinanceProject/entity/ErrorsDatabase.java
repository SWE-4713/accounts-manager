package com.example.FinanceProject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Errors")
@Getter
@Setter
public class ErrorsDatabase {
    @Id
    @Column(name = "Error Id")
    private int id;

    @Column(name = "Error Description")
    private String description;
}


