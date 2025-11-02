package com.example.hack1.DTO.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponseDTO {
    private String id;
    private String sku;
    private BigDecimal price;
    private Integer units;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private String soldAt;

    private String branch;
}
