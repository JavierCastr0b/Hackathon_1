package com.example.hack1.sales.application;

import com.example.hack1.sales.domain.SaleRequest;
import com.example.hack1.sales.domain.Sales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Service
public class SalesController {
    @Autowired
    private SalesService salesService;

    private boolean isCentral(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equalsIgnoreCase("CENTRAL") || a.equalsIgnoreCase("ROLE_CENTRAL"));
    }

    private String userBranch(Authentication auth) {
        Optional<String> byAuth = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.toUpperCase().startsWith("BRANCH_"))
                .map(a -> a.substring(a.indexOf('_') + 1))
                .findFirst();
        return byAuth.orElse(null);
    }

    @PostMapping
    public ResponseEntity<Sales> createSale(@RequestBody SaleRequest req, Authentication auth) {
        boolean central = isCentral(auth);
        String branch = userBranch(auth);
        Sales s = salesService.createSale(req, auth.getName(), central, branch);
        return ResponseEntity.status(HttpStatus.CREATED).body(s);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Sales> getSale(@PathVariable String id, Authentication auth) {
        boolean central = isCentral(auth);
        String branch = userBranch(auth);
        Sales s = salesService.getSale(id, central, branch);
        return ResponseEntity.ok(s);
    }

    @GetMapping
    public ResponseEntity<Page<Sales>> listSales(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        boolean central = isCentral(auth);
        String userBranch = userBranch(auth);
        Page<Sales> result = salesService.listSales(from, to, branch, page, size, central, userBranch);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sales> updateSale(@PathVariable String id, @RequestBody SaleRequest req, Authentication auth) {
        boolean central = isCentral(auth);
        String branch = userBranch(auth);
        Sales updated = salesService.updateSale(id, req, central, branch);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSale(@PathVariable String id, Authentication auth) {
        boolean central = isCentral(auth);
        salesService.deleteSale(id, central);
    }


}
