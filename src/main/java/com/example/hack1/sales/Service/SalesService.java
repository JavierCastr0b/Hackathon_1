package com.example.hack1.sales.Service;

import com.example.hack1.DTO.Request.SaleRequestDTO;
import com.example.hack1.DTO.Response.SaleResponseDTO;
import com.example.hack1.Exception.ResourceNotFoundException;
import com.example.hack1.Exception.UnauthorizedException;
import com.example.hack1.Models.GitHubModelsService;
import com.example.hack1.User.Repository.UserRepository;
import com.example.hack1.User.domain.User;
import com.example.hack1.sales.Repository.SalesRepository;
import com.example.hack1.sales.domain.Sales;
import com.example.hack1.sales.domain.SalesAggregates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;
    private final UserRepository userRepository;
    private final GitHubModelsService gitHubModelsService;
    private final ModelMapper modelMapper;

    /**
     * Obtener el usuario autenticado actual
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no autenticado"));
    }


    public boolean isCentral(User user) {
        return "CENTRAL".equals(user.getRole().name());
    }

    private Instant parseSoldAt(String soldAt) {
        if (soldAt == null) return null;
        return Instant.parse(soldAt); // Lanza DateTimeParseException si es inválido
    }

    /**
     * POST /api/sales - Crear una nueva venta
     * CENTRAL: puede crear ventas para cualquier sucursal
     * BRANCH: solo puede crear ventas para su propia sucursal
     */
    @Transactional
    public SaleResponseDTO createSale(SaleRequestDTO request) {
        User currentUser = getCurrentUser();
        boolean isCentral = isCentral(currentUser);

        // Validar permisos de sucursal
        if (!isCentral) {
            String userBranch = currentUser.getBranch();
            if (!request.getBranch().equalsIgnoreCase(userBranch)) {
                throw new UnauthorizedException("Usuario BRANCH solo puede crear ventas para su sucursal: " + userBranch);
            }
        }

        Sales sale = Sales.builder()
                .sku(request.getSku())
                .units(request.getUnits())
                .price(request.getPrice())
                .branch(request.getBranch())
                .soldAt(parseSoldAt(request.getSoldAt()))
                .createdByUser(currentUser)
                .build();

        Sales savedSale = salesRepository.save(sale);
        log.info("Venta creada: {} por usuario: {}", savedSale.getId(), currentUser.getUsername());

        return modelMapper.map(savedSale, SaleResponseDTO.class);
    }

    /**
     * GET /api/sales/:id - Obtener una venta específica
     * CENTRAL: puede ver cualquier venta
     * BRANCH: solo puede ver ventas de su sucursal
     */
    @Transactional(readOnly = true)
    public SaleResponseDTO getSale(String id) {
        User currentUser = getCurrentUser();
        boolean isCentral = isCentral(currentUser);

        Sales sale = salesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));

        // Validar permisos
        if (!isCentral && !sale.getBranch().equalsIgnoreCase(currentUser.getBranch())) {
            throw new UnauthorizedException("Acceso denegado a venta de otra sucursal");
        }

        return modelMapper.map(sale, SaleResponseDTO.class);
    }

    /**
     * GET /api/sales - Listar ventas con paginación y filtros
     * CENTRAL: puede ver todas las ventas, filtrar por sucursal opcional
     * BRANCH: solo ve ventas de su sucursal
     */
    @Transactional(readOnly = true)
    public Page<SaleResponseDTO> listSales(String from, String to, String branch, int page, int size) {
        User currentUser = getCurrentUser();
        boolean isCentral = isCentral(currentUser);

        // Si es BRANCH, forzar su sucursal
        if (!isCentral) {
            branch = currentUser.getBranch();
        }

        Instant fromInstant = parseSoldAt(from);
        Instant toInstant = parseSoldAt(to);
        Pageable pageable = PageRequest.of(page, size);

        Page<Sales> salesPage;

        // Filtrar según parámetros
        if (fromInstant != null && toInstant != null && branch != null && !branch.isBlank()) {
            salesPage = salesRepository.findByBranchAndSoldAtBetween(branch, fromInstant, toInstant, pageable);
        } else if (fromInstant != null && toInstant != null) {
            salesPage = salesRepository.findBySoldAtBetween(fromInstant, toInstant, pageable);
        } else if (branch != null && !branch.isBlank()) {
            salesPage = salesRepository.findByBranch(branch, pageable);
        } else {
            salesPage = salesRepository.findAll(pageable);
        }

        // Convertir a DTO usando ModelMapper
        return salesPage.map(sale -> modelMapper.map(sale, SaleResponseDTO.class));
    }

    /**
     * PUT /api/sales/:id - Actualizar una venta
     * CENTRAL: puede actualizar cualquier venta
     * BRANCH: solo puede actualizar ventas de su sucursal (no puede cambiar de sucursal)
     */
    @Transactional
    public SaleResponseDTO updateSale(String id, SaleRequestDTO request) {
        User currentUser = getCurrentUser();
        boolean isCentral = isCentral(currentUser);

        Sales existingSale = salesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));

        // Validar permisos de acceso
        if (!isCentral && !existingSale.getBranch().equalsIgnoreCase(currentUser.getBranch())) {
            throw new UnauthorizedException("Acceso denegado a venta de otra sucursal");
        }

        // Validar cambio de sucursal
        if (!isCentral) {
            if (request.getBranch() != null &&
                    !request.getBranch().equalsIgnoreCase(currentUser.getBranch())) {
                throw new UnauthorizedException("Usuario BRANCH no puede mover venta a otra sucursal");
            }
            // Mantener la sucursal actual
            existingSale.setBranch(currentUser.getBranch());
        } else {
            // CENTRAL puede cambiar de sucursal
            if (request.getBranch() != null) {
                existingSale.setBranch(request.getBranch());
            }
        }

        // Actualizar campos
        if (request.getSku() != null) existingSale.setSku(request.getSku());
        if (request.getUnits() != null) existingSale.setUnits(request.getUnits());
        if (request.getPrice() != null) existingSale.setPrice(request.getPrice());
        if (request.getSoldAt() != null) existingSale.setSoldAt(parseSoldAt(request.getSoldAt()));

        Sales updatedSale = salesRepository.save(existingSale);
        log.info("Venta actualizada: {} por usuario: {}", updatedSale.getId(), currentUser.getUsername());

        return modelMapper.map(updatedSale, SaleResponseDTO.class);
    }

    /**
     * DELETE /api/sales/:id - Eliminar una venta
     * Solo CENTRAL puede eliminar ventas
     */
    @Transactional
    public void deleteSale(String id) {
        User currentUser = getCurrentUser();

        if (!isCentral(currentUser)) {
            throw new UnauthorizedException("Solo usuarios CENTRAL pueden eliminar ventas");
        }

        if (!salesRepository.existsById(id)) {
            throw new ResourceNotFoundException("Venta no encontrada");
        }

        salesRepository.deleteById(id);
        log.info("Venta eliminada: {} por usuario: {}", id, currentUser.getUsername());
    }

    @Transactional(readOnly = true)
    public SalesAggregates calculateAggregatesForReport(LocalDate from, LocalDate to, String branch) {
        // Si from y to son null, calcular la última semana
        if (from == null || to == null) {
            to = LocalDate.now();
            from = to.minusDays(7);
        }

        Instant startInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

        Page<Sales> salesPage;
        boolean isFilteredByBranch = (branch != null && !branch.isBlank());

        if (isFilteredByBranch) {
            salesPage = salesRepository.findByBranchAndSoldAtBetween(
                    branch, startInstant, endInstant, Pageable.unpaged()
            );
        } else {
            salesPage = salesRepository.findBySoldAtBetween(
                    startInstant, endInstant, Pageable.unpaged()
            );
        }

        List<Sales> salesList = salesPage.getContent();

        if (salesList.isEmpty()) {
            return new SalesAggregates(0, 0, 0.0, "N/A", "N/A", from, to, branch,
                    "No hay ventas registradas en este período.");
        }

        int totalSales = salesList.size();
        int totalUnits = salesList.stream().mapToInt(Sales::getUnits).sum();

        BigDecimal totalRevenue = salesList.stream()
                .map(s -> s.getPrice().multiply(BigDecimal.valueOf(s.getUnits())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String topSku = salesList.stream()
                .collect(Collectors.groupingBy(Sales::getSku, Collectors.summingInt(Sales::getUnits)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topBranch = isFilteredByBranch ? branch :
                salesList.stream()
                        .collect(Collectors.groupingBy(Sales::getBranch, Collectors.summingInt(Sales::getUnits)))
                        .entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("N/A");

        // ✅ Generar resumen con IA
        String summary = gitHubModelsService.generateSummary(
                totalUnits,
                totalRevenue.doubleValue(),
                topSku,
                topBranch
        );

        return new SalesAggregates(
                totalSales, totalUnits, totalRevenue.doubleValue(),
                topSku, topBranch, from, to, branch, summary
        );
    }
}