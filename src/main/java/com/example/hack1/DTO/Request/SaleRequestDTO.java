package com.example.hack1.DTO.Request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDTO {

    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotNull(message = "Las unidades son obligatorias")
    @Min(value = 1, message = "Debe vender al menos 1 unidad")
    private Integer units;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    @NotBlank(message = "La sucursal es obligatoria")
    private String branch;

    @NotBlank(message = "La fecha de venta es obligatoria")
    private String soldAt;  // ISO-8601 format
}
