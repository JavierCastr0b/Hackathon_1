package com.example.hack1.sales.application;

import com.example.hack1.sales.domain.SaleRequest;
import com.example.hack1.sales.domain.Sales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
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
        List<Sales> filtered = all.stream()
                .filter(s -> branch == null || branch.isEmpty() || branch.equalsIgnoreCase(s.getBranch()))
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
    
}
