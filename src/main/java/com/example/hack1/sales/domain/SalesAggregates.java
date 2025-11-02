package com.example.hack1.sales.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesAggregates {
    private int totalSales;
    private int totalUnits;
    private double totalRevenue;
    private String topSku;
    private String topBranch;
    private LocalDate from;
    private LocalDate to;
    private String branch;
    private String summary;  // ✅ AGREGAR ESTE CAMPO
}
