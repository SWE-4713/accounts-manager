package com.example.FinanceProject.repository;

import com.example.FinanceProject.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    List<User> findByRoleAndStatusNot(String role, String status);
}
