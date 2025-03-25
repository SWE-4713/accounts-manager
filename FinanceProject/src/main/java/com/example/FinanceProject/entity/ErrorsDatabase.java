package com.example.FinanceProject.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Errors")
@Data
public class ErrorsDatabase {
    @Id
    @Column(name = "Error Id")
    private int id;
    @Column(name = "Error Description")
    private String description;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }


}


