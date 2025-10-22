package com.example.hack1.sales.domain;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SaleRequest {
    @NotBlank
    private String sku;

    @NotNull
    @Min(1)
    private Integer units;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotBlank
    private String branch;

    @NotBlank
    private String soldAt;

    public SaleRequest() {
    }

    public SaleRequest(String sku, Integer units, BigDecimal price, String branch, String soldAt) {
        this.sku = sku;
        this.units = units;
        this.price = price;
        this.branch = branch;
        this.soldAt = soldAt;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getUnits() {
        return units;
    }
    public void setUnits(Integer units) {
        this.units = units;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(String soldAt) {
        this.soldAt = soldAt;
    }
}
