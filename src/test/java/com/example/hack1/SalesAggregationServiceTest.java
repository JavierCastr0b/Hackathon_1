package com.example.hack1;

import com.example.hack1.sales.application.SalesRepository;
import com.example.hack1.sales.application.SalesService;
import com.example.hack1.sales.domain.Sales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Imports necesarios para Page, PageImpl e Instant
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
// -----

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    private SalesRepository salesRepository;

    @InjectMocks
    private SalesService salesService;

    private static final LocalDate START_DATE = LocalDate.parse("2025-09-01");
    private static final LocalDate END_DATE = LocalDate.parse("2025-09-07");

    private List<Sales> allMockSales;
    private Page<Sales> mockSalesPage; // <-- Objeto Page para simular la respuesta
    private Page<Sales> emptyPage; // <-- Página vacía

    @BeforeEach
    void setUp() {
        // GIVEN: Creamos la lista de entidades "Sales"
        allMockSales = List.of(
                createSales("s1", "OREO_CLASSIC_12", 25, 1.99, "Miraflores", "2025-09-01T10:30:00Z"),
                createSales("s2", "OREO_DOUBLE", 40, 2.49, "Miraflores", "2025-09-02T15:10:00Z"),
                createSales("s3", "OREO_THINS", 32, 2.19, "San Isidro", "2025-09-03T11:05:00Z"),
                createSales("s4", "OREO_DOUBLE", 55, 2.49, "San Isidro", "2025-09-04T18:50:00Z"),
                createSales("s5", "OREO_CLASSIC_12", 20, 1.99, "Miraflores", "2025-09-05T09:40:00Z")
        );

        // Creamos una "Page" falsa que contiene nuestra lista
        mockSalesPage = new PageImpl<>(allMockSales);
        emptyPage = new PageImpl<>(Collections.emptyList());
    }

    /**
     * Test 1: (REQUERIDO) Test de agregados con datos válidos (Global)
     */
    @Test
    @DisplayName("1. Debe calcular agregados globales correctos con datos válidos")
    void shouldCalculateCorrectGlobalAggregatesWithValidData() {
        // GIVEN: Simulamos el método del repo que usa Instant y Pageable
        when(salesRepository.findBySoldAtBetween(
                any(Instant.class), any(Instant.class), any(Pageable.class)
        )).thenReturn(mockSalesPage); // <-- Devolvemos la Página simulada

        // WHEN: Llamamos al método en SalesService
        SalesAggregates result = salesService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN: Las validaciones son las mismas
        assertThat(result.totalUnits()).isEqualTo(172);
        assertThat(result.totalRevenue()).isEqualTo(396.18);
        assertThat(result.topSku()).isEqualTo("OREO_DOUBLE");
        assertThat(result.topBranch()).isEqualTo("San Isidro");
    }

    /**
     * Test 2: (REQUERIDO) Test con lista vacía
     */
    @Test
    @DisplayName("2. Debe devolver agregados vacíos (0 y N/A) si no hay ventas")
    void shouldReturnEmptyAggregatesWhenNoSalesFound() {
        // GIVEN: Devolvemos una Página vacía
        when(salesRepository.findBySoldAtBetween(
                any(Instant.class), any(Instant.class), any(Pageable.class)
        )).thenReturn(emptyPage);

        // WHEN
        SalesAggregates result = salesService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN
        assertThat(result.totalUnits()).isZero();
        assertThat(result.totalRevenue()).isZero();
        assertThat(result.topSku()).isEqualTo("N/A");
    }

    /**
     * Test 3: (REQUERIDO) Test de filtrado por sucursal
     */
    @Test
    @DisplayName("3. Debe calcular agregados correctos filtrando por sucursal (BRANCH)")
    void shouldCalculateCorrectAggregatesWithBranchFilter() {
        // GIVEN
        String branch = "Miraflores";
        List<Sales> mirafloresSalesList = allMockSales.stream()
                .filter(sale -> branch.equals(sale.getBranch()))
                .toList();
        Page<Sales> mirafloresPage = new PageImpl<>(mirafloresSalesList); // <-- Página filtrada

        // Simulamos el método específico del repo CON branch
        when(salesRepository.findByBranchAndSoldAtBetween(
                eq(branch), any(Instant.class), any(Instant.class), any(Pageable.class)
        )).thenReturn(mirafloresPage); // <-- Devolvemos la página de Miraflores

        // WHEN
        SalesAggregates result = salesService.calculateAggregates(START_DATE, END_DATE, branch);

        // THEN
        assertThat(result.totalUnits()).isEqualTo(85);
        assertThat(result.totalRevenue()).isEqualTo(189.15);
        assertThat(result.topSku()).isEqualTo("OREO_CLASSIC_12");
    }

    /**
     * Test 4: (REQUERIDO) Test de filtrado por fechas
     * (Este test ahora solo verifica que se llame al repo,
     * asumimos que el service convierte LocalDate -> Instant)
     */
    @Test
    @DisplayName("4. Debe llamar al repositorio con los argumentos correctos")
    void shouldCallRepositoryWithCorrectArguments() {
        // GIVEN (Setup de una página vacía para evitar NPE)
        when(salesRepository.findBySoldAtBetween(
                any(Instant.class), any(Instant.class), any(Pageable.class)
        )).thenReturn(emptyPage);

        // WHEN
        salesService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN: Verificamos que SalesService llamó al repositorio
        // con los tipos de dato correctos (Instant y Pageable)
        verify(salesRepository).findBySoldAtBetween(
                any(Instant.class), any(Instant.class), any(Pageable.class)
        );
    }

    /**
     * Test 5: (REQUERIDO) Test de cálculo de SKU top con empates
     */
    @Test
    @DisplayName("5. Debe manejar empates en Top SKU correctamente")
    void shouldCorrectlyIdentifyTopSkuWithTies() {
        // GIVEN
        List<Sales> tieSalesList = List.of(
                createSales("s1", "OREO_A", 20, 1.0, "A", "2025-09-01T10:00:00Z"),
                createSales("s2", "OREO_B", 20, 1.0, "A", "2025-09-01T11:00:00Z"),
                createSales("s3", "OREO_C", 10, 1.0, "A", "2025-09-01T12:00:00Z")
        );
        Page<Sales> tiePage = new PageImpl<>(tieSalesList);

        when(salesRepository.findBySoldAtBetween(
                any(Instant.class), any(Instant.class), any(Pageable.class)
        )).thenReturn(tiePage);

        // WHEN
        SalesAggregates result = salesService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN
        assertThat(result.topSku()).isIn("OREO_A", "OREO_B");
        assertThat(result.totalUnits()).isEqualTo(50);
    }

    // --- Método Helper para crear tu entidad "Sales" ---

    private Sales createSales(String id, String sku, int units, double price, String branch, String soldAtStr) {
        Sales sale = new Sales(); // <-- Usando tu clase "Sales"
        sale.setId(id);
        sale.setSku(sku);
        sale.setUnits(units);
        sale.setPrice(price);
        sale.setBranch(branch);
        // Convertimos el String a Instant (asumiendo UTC)
        sale.setSoldAt(Instant.parse(soldAtStr));
        return sale;
    }
}