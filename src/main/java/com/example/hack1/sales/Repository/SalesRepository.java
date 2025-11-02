package com.example.hack1.sales.Repository;

import com.example.hack1.sales.domain.Sales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SalesRepository extends JpaRepository<Sales, String> {
    Page<Sales> findBySoldAtBetween(Instant from, Instant to, Pageable pageable);

    Page<Sales> findByBranchAndSoldAtBetween(String branch, Instant from, Instant to, Pageable pageable);

    Page<Sales> findByBranch(String branch, Pageable pageable);
}
