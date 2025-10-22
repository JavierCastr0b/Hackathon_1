package com.example.hack1.sales.application;

import com.example.hack1.sales.domain.SaleRequest;
import com.example.hack1.sales.domain.Sales;
import com.example.hack1.sales.domain.SalesAggregates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class SalesService {
    @Autowired
    private SalesRepository saleRepository;

    private Instant parseSoldAt(String soldAt) {
        if (soldAt == null) return null;
        try {
            return Instant.parse(soldAt);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "soldAt debe ser ISO-8601");
        }
    }

    public void recordSale(SaleRequest saleRequest) {
        if (saleRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "saleRequest no puede ser null");
        }

        Sales s = new Sales();
        s.setSku(saleRequest.getSku());
        s.setUnits(saleRequest.getUnits());
        s.setPrice(saleRequest.getPrice());
        s.setBranch(saleRequest.getBranch());
        s.setSoldAt(parseSoldAt(saleRequest.getSoldAt()));

        saleRepository.save(s);
    }

    public Sales createSale(SaleRequest req, String name, boolean central, String userBranch) {
        if (!central) {
            String targetBranch = req.getBranch();
            if (targetBranch == null || !targetBranch.equalsIgnoreCase(userBranch)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario BRANCH solo puede crear ventas para su sucursal");
            }
        }

        Sales s = new Sales();
        s.setSku(req.getSku());
        s.setUnits(req.getUnits());
        s.setPrice(req.getPrice());
        s.setBranch(req.getBranch());
        s.setSoldAt(parseSoldAt(req.getSoldAt()));
        s.setCreatedBy(name);
        return saleRepository.save(s);
    }

    public Sales getSale(String id, boolean central, String userBranch) {
        Sales s = saleRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        if (!central && !Objects.equals(s.getBranch(), userBranch)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a venta de otra sucursal");
        }
        return s;
    }

    public Page<Sales> listSales(String fromIso, String toIso, String branch, int page, int size, boolean central, String userBranch) {
        Instant from = parseSoldAt(fromIso);
        Instant to = parseSoldAt(toIso);

        if (!central) {
            branch = userBranch;
        }

        List<Sales> all = saleRepository.findAll();
        String finalBranch = branch;
        List<Sales> filtered = all.stream()
                .filter(s -> finalBranch == null || finalBranch.isEmpty() || finalBranch.equalsIgnoreCase(s.getBranch()))
                .filter(s -> from == null || !s.getSoldAt().isBefore(from))
                .filter(s -> to == null || !s.getSoldAt().isAfter(to))
                .collect(Collectors.toList());

        int start = Math.max(0, page * size);
        int end = Math.min(filtered.size(), start + size);
        List<Sales> content = start < end ? filtered.subList(start, end) : List.of();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return new PageImpl<>(content, pageable, filtered.size());
    }

    public Sales updateSale(String id, SaleRequest req, boolean central, String userBranch) {
        Sales existing = getSale(id, central, userBranch);

        if (!central) {
            String reqBranch = req.getBranch();
            if (reqBranch != null && !reqBranch.equalsIgnoreCase(userBranch)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario BRANCH no puede mover venta a otra sucursal");
            }
            existing.setBranch(userBranch);
        } else {
            if (req.getBranch() != null) existing.setBranch(req.getBranch());
        }

        if (req.getSku() != null) existing.setSku(req.getSku());
        if (req.getUnits() != null) existing.setUnits(req.getUnits());
        if (req.getPrice() != null) existing.setPrice(req.getPrice());
        if (req.getSoldAt() != null) existing.setSoldAt(parseSoldAt(req.getSoldAt()));

        return saleRepository.save(existing);
    }


    public void deleteSale(String id, boolean central) {
        if (!central) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo usuarios CENTRAL pueden eliminar ventas");
        }
        if (!saleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada");
        }
        saleRepository.deleteById(id);
    }

    public SalesAggregates calculateAggregates(LocalDate from, LocalDate to, String branch) {

        Instant startInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();

        Instant endInstant = to.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

        Page<Sales> salesPage;
        boolean isFilteredByBranch = (branch != null && !branch.isBlank());

        if (isFilteredByBranch) {
            salesPage = saleRepository.findByBranchAndSoldAtBetween(
                    branch, startInstant, endInstant, Pageable.unpaged()
            );
        } else {
            salesPage = saleRepository.findBySoldAtBetween(
                    startInstant, endInstant, Pageable.unpaged()
            );
        }

        List<Sales> salesList = salesPage.getContent();

        if (salesList.isEmpty()) {
            return new SalesAggregates(
                    0, 0, 0.0, "N/A", "N/A", from, to, branch
            );
        }

        int totalSales = salesList.size();
        int totalUnits = salesList.stream()
                .mapToInt(Sales::getUnits)
                .sum();

        double totalRevenue = salesList.stream()
                .mapToDouble(s -> s.getUnits() * s.getPrice())
                .sum();
        totalRevenue = Math.round(totalRevenue * 100.0) / 100.0;

        String topSku = salesList.stream()
                .collect(Collectors.groupingBy(
                        Sales::getSku, // Agrupar por nombre de SKU
                        Collectors.summingInt(Sales::getUnits) // Sumar las unidades
                ))
                .entrySet().stream() // Obtener un stream de [SKU, TotalUnidades]
                .max(Map.Entry.comparingByValue()) // Encontrar el máximo por valor (TotalUnidades)
                .map(Map.Entry::getKey) // Quedarnos solo con el nombre (la clave)
                .orElse("N/A"); // Fallback por si acaso

        String topBranch;
        if (isFilteredByBranch) {
            topBranch = branch; // Si ya filtramos, esa es la "top"
        } else {
            topBranch = salesList.stream()
                    .collect(Collectors.groupingBy(
                            Sales::getBranch,
                            Collectors.summingInt(Sales::getUnits)
                    ))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");
        }

        return new SalesAggregates(
                totalSales,
                totalUnits,
                totalRevenue,
                topSku,
                topBranch,
                from,
                to,
                branch
        );
    }
    
}
