package com.example.hack1.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySummaryResponseDTO {
    private String requestId;
    private String status;
    private String message;
    private String estimatedTime;
    private Instant requestedAt;
}