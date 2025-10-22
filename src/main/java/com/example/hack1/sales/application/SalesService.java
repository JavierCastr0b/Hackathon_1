package com.example.hack1.sales.application;

import com.example.hack1.sales.domain.SaleRequest;
import com.example.hack1.sales.domain.Sales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeParseException;


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
    
}
