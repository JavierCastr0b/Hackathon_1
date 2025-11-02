package com.example.hack1.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestedEvent {
    private String requestId;
    private LocalDate from;
    private LocalDate to;
    private String branch;
    private String emailTo;
    private String requestedBy;
    private boolean isPremium;
    private boolean includeCharts;
    private boolean attachPdf;
}
