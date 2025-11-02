package com.example.hack1.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySummaryRequestDTO {

    private LocalDate from;
    private LocalDate to;
    private String branch;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String emailTo;
}
