package com.example.hack1.sales.domain;

import java.time.LocalDate;

public record SalesAggregates(
        int totalSales,      // Cantidad total de tickets de venta
        int totalUnits,      // Suma total de unidades vendidas
        double totalRevenue, // Suma total de ingresos (unidades * precio)
        String topSku,       // El SKU más vendido por unidades
        String topBranch,    // La sucursal con más unidades vendidas

        // Incluimos los parámetros de entrada para dárselos al LLM
        LocalDate from,
        LocalDate to,
        String filteredBranch // La sucursal filtrada, o 'null' si es global
) {}
