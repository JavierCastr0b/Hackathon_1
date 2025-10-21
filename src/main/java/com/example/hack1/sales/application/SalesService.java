package com.example.hack1.sales.application;

import com.example.hack1.sales.domain.SaleRequest;
import com.example.hack1.sales.domain.Sales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;


@Service
public class SalesService {
    @Autowired
    private SalesRepository saleRepository;

    public void recordSale(SaleRequest saleRequest) {
    }

    public Sales createSale(SaleRequest req, String name, boolean central, String branch) {
    }

    public Sales getSale(String id, boolean central, String branch) {
    }

    public Page<Sales> listSales(String from, String to, String branch, int page, int size, boolean central, String userBranch) {
    }

    public Sales updateSale(String id, SaleRequest req, boolean central, String branch) {
    }

    public void deleteSale(String id, boolean central) {
    }
}
