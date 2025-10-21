package com.example.hack1.sales.application;

import com.example.hack1.sales.domain.Sales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;

public interface SalesRepository extends CrudRepository<Sales,String> {
    Page<Sales> findBySoldAtBetween(Instant from, Instant to, Pageable pageable);
    Page<Sales> findByBranchAndSoldAtBetween(String branch, Instant from, Instant to, Pageable pageable);
}
