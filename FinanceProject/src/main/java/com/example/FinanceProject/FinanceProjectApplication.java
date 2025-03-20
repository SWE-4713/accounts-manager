package com.example.FinanceProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinanceProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceProjectApplication.class, args);
	}

}
