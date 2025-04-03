package com.example.FinanceProject.repository;

import com.example.FinanceProject.entity.ErrorsDatabase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorDatabaseRepo extends JpaRepository<ErrorsDatabase, Integer> {
}
