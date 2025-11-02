package com.example.hack1.sales.Controller;

import com.example.hack1.DTO.Request.PremiumWeeklySummaryRequestDTO;
import com.example.hack1.DTO.Request.ReportRequestedEvent;
import com.example.hack1.DTO.Request.SaleRequestDTO;
import com.example.hack1.DTO.Request.WeeklySummaryRequestDTO;
import com.example.hack1.DTO.Response.PremiumWeeklySummaryResponseDTO;
import com.example.hack1.DTO.Response.SaleResponseDTO;
import com.example.hack1.DTO.Response.WeeklySummaryResponseDTO;
import com.example.hack1.User.domain.User;
import com.example.hack1.sales.Service.SalesService;
import com.example.hack1.sales.domain.Sales;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;
    private final ApplicationEventPublisher applicationEventPublisher;  // ✅ AGREGAR ESTA LÍNEA


    /**
     * POST /api/sales
     * Crear una nueva venta
     * CENTRAL: puede crear para cualquier sucursal
     * BRANCH: solo puede crear para su sucursal
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CENTRAL', 'BRANCH')")
    public ResponseEntity<SaleResponseDTO> createSale(@Valid @RequestBody SaleRequestDTO request) {
        SaleResponseDTO response = salesService.createSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/sales/{id}
     * Obtener detalle de una venta específica
     * CENTRAL: puede ver cualquier venta
     * BRANCH: solo puede ver ventas de su sucursal
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CENTRAL', 'BRANCH')")
    public ResponseEntity<SaleResponseDTO> getSale(@PathVariable String id) {
        SaleResponseDTO response = salesService.getSale(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/sales
     * Listar ventas con filtros opcionales y paginación
     * Query params: from, to, branch, page, size
     * CENTRAL: puede ver todas las ventas y filtrar por sucursal
     * BRANCH: solo puede ver ventas de su sucursal
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CENTRAL', 'BRANCH')")
    public ResponseEntity<Page<SaleResponseDTO>> listSales(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SaleResponseDTO> sales = salesService.listSales(from, to, branch, page, size);
        return ResponseEntity.ok(sales);
    }

    /**
     * PUT /api/sales/{id}
     * Actualizar una venta existente
     * CENTRAL: puede actualizar cualquier venta
     * BRANCH: solo puede actualizar ventas de su sucursal (no puede cambiar branch)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CENTRAL', 'BRANCH')")
    public ResponseEntity<SaleResponseDTO> updateSale(
            @PathVariable String id,
            @Valid @RequestBody SaleRequestDTO request) {
        SaleResponseDTO response = salesService.updateSale(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/sales/{id}
     * Eliminar una venta
     * Solo CENTRAL puede eliminar ventas
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CENTRAL')")
    public ResponseEntity<Void> deleteSale(@PathVariable String id) {
        salesService.deleteSale(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/summary/weekly")
    @PreAuthorize("hasAnyAuthority('CENTRAL', 'BRANCH')")
    public ResponseEntity<WeeklySummaryResponseDTO> requestWeeklySummary(
            @Valid @RequestBody WeeklySummaryRequestDTO request) {
        return ResponseEntity.accepted().body(salesService.requestWeeklySummary(request));
    }

    @PostMapping("/summary/weekly/premium")
    @PreAuthorize("hasAnyAuthority('CENTRAL', 'BRANCH')")
    public ResponseEntity<PremiumWeeklySummaryResponseDTO> requestPremiumWeeklySummary(
            @Valid @RequestBody PremiumWeeklySummaryRequestDTO request) {
        return ResponseEntity.accepted().body(salesService.requestPremiumWeeklySummary(request));
    }
}