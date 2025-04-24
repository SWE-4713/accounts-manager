// src/main/java/com/example/FinanceProject/dto/RatioInfo.java
package com.example.FinanceProject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatioInfo {
    private String name;
    private String value; // Store formatted value as String
    private String color; // "green", "yellow", "red"
    private String description; // Optional: Description of the ratio
}