package com.example.hack1;

import com.example.hack1.Models.GitHubModelsService;
import com.example.hack1.User.Repository.UserRepository;
import com.example.hack1.sales.Repository.SalesRepository;
import com.example.hack1.sales.Service.SalesService;
import com.example.hack1.sales.domain.Sales;
import com.example.hack1.sales.domain.SalesAggregates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de Cálculo de Agregados de Ventas")
class SalesServiceTest {

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private GitHubModelsService gitHubModelsService;

    @InjectMocks
    private SalesService salesService;

    private LocalDate fromDate;
    private LocalDate toDate;
    private Instant fromInstant;
    private Instant toInstant;

    @BeforeEach
    void setUp() {
        fromDate = LocalDate.of(2025, 11, 1);
        toDate = LocalDate.of(2025, 11, 7);
        fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        toInstant = toDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
    }

    /**
     * TEST 1: Test de agregados con datos válidos
     */
    @Test
    @DisplayName("Debe calcular agregados correctamente con datos válidos")
    void shouldCalculateCorrectAggregatesWithValidData() {

        List<Sales> mockSales = List.of(
                createSale("s_001", "OREO_CLASSIC", 10, 1.99, "Miraflores", fromInstant),
                createSale("s_002", "OREO_DOUBLE", 5, 2.49, "San Isidro", fromInstant.plusSeconds(3600)),
                createSale("s_003", "OREO_CLASSIC", 15, 1.99, "Miraflores", fromInstant.plusSeconds(7200))
        );

        Page<Sales> mockPage = new PageImpl<>(mockSales);

        when(salesRepository.findBySoldAtBetween(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(mockPage);

        when(gitHubModelsService.generateSummary(eq(30), eq(62.20), eq("OREO_CLASSIC"), eq("Miraflores")))
                .thenReturn("Resumen generado por IA");


        SalesAggregates result = salesService.calculateAggregatesForReport(fromDate, toDate, null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalSales()).isEqualTo(3);
        assertThat(result.getTotalUnits()).isEqualTo(30);
        assertThat(result.getTotalRevenue()).isEqualTo(62.20);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
        assertThat(result.getSummary()).isEqualTo("Resumen generado por IA");
    }

    /**
     * TEST 2: Test con lista vacía
     */
    @Test
    @DisplayName("Debe retornar valores por defecto cuando no hay ventas")
    void shouldReturnDefaultValuesWhenNoSales() {
        Page<Sales> emptyPage = new PageImpl<>(new ArrayList<>());

        when(salesRepository.findBySoldAtBetween(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        SalesAggregates result = salesService.calculateAggregatesForReport(fromDate, toDate, null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalSales()).isEqualTo(0);
        assertThat(result.getTotalUnits()).isEqualTo(0);
        assertThat(result.getTotalRevenue()).isEqualTo(0.0);
        assertThat(result.getTopSku()).isEqualTo("N/A");
        assertThat(result.getTopBranch()).isEqualTo("N/A");
        assertThat(result.getSummary()).isEqualTo("No hay ventas registradas en este período.");
    }

    /**
     * TEST 3: Test de filtrado por sucursal
     */
    @Test
    @DisplayName("Debe filtrar correctamente por sucursal")
    void shouldFilterSalesByBranch() {
        List<Sales> mockSales = List.of(
                createSale("s_001", "OREO_CLASSIC", 10, 1.99, "Lima", fromInstant),
                createSale("s_002", "OREO_DOUBLE", 5, 2.49, "Lima", fromInstant.plusSeconds(3600)),
                createSale("s_003", "OREO_GOLDEN", 8, 2.99, "Lima", fromInstant.plusSeconds(7200))
        );

        Page<Sales> mockPage = new PageImpl<>(mockSales);

        when(salesRepository.findByBranchAndSoldAtBetween(
                eq("Lima"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(mockPage);

        when(gitHubModelsService.generateSummary(eq(23), eq(56.27), eq("OREO_CLASSIC"), eq("Lima")))
                .thenReturn("Resumen de Lima");

        SalesAggregates result = salesService.calculateAggregatesForReport(fromDate, toDate, "Lima");

        assertThat(result).isNotNull();
        assertThat(result.getTotalSales()).isEqualTo(3);
        assertThat(result.getTotalUnits()).isEqualTo(23);
        assertThat(result.getTotalRevenue()).isEqualTo(56.27);
        assertThat(result.getBranch()).isEqualTo("Lima");
        assertThat(result.getTopBranch()).isEqualTo("Lima");
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
    }

    /**
     * TEST 4: Test de filtrado por fechas
     */
    @Test
    @DisplayName("Debe filtrar correctamente por rango de fechas")
    void shouldFilterSalesByDateRange() {
        LocalDate specificFrom = LocalDate.of(2025, 11, 1);
        LocalDate specificTo = LocalDate.of(2025, 11, 3);

        List<Sales> mockSales = List.of(
                createSale("s_001", "OREO_CLASSIC", 10, 1.99, "Miraflores", specificFrom.atStartOfDay(ZoneOffset.UTC).toInstant()),
                createSale("s_002", "OREO_DOUBLE", 5, 2.49, "Miraflores", specificFrom.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(86400))
        );

        Page<Sales> mockPage = new PageImpl<>(mockSales);

        when(salesRepository.findBySoldAtBetween(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(mockPage);

        when(gitHubModelsService.generateSummary(eq(15), eq(32.35), eq("OREO_CLASSIC"), eq("Miraflores")))
                .thenReturn("Resumen del rango");

        SalesAggregates result = salesService.calculateAggregatesForReport(specificFrom, specificTo, null);

        assertThat(result).isNotNull();
        assertThat(result.getFrom()).isEqualTo(specificFrom);
        assertThat(result.getTo()).isEqualTo(specificTo);
        assertThat(result.getTotalSales()).isEqualTo(2);
        assertThat(result.getTotalUnits()).isEqualTo(15);
        assertThat(result.getTotalRevenue()).isEqualTo(32.35);
    }

    /**
     * TEST 5: Test de cálculo de SKU top
     */
    @Test
    @DisplayName("Debe identificar correctamente el SKU más vendido incluso con empates")
    void shouldIdentifyTopSkuCorrectlyEvenWithTies() {
        List<Sales> mockSales = List.of(
                createSale("s_001", "OREO_CLASSIC", 10, 1.99, "Miraflores", fromInstant),
                createSale("s_002", "OREO_DOUBLE", 15, 2.49, "Miraflores", fromInstant.plusSeconds(3600)),
                createSale("s_003", "OREO_GOLDEN", 8, 2.99, "Miraflores", fromInstant.plusSeconds(7200)),
                createSale("s_004", "OREO_DOUBLE", 10, 2.49, "Miraflores", fromInstant.plusSeconds(10800)),
                createSale("s_005", "OREO_CLASSIC", 5, 1.99, "Miraflores", fromInstant.plusSeconds(14400))
        );

        Page<Sales> mockPage = new PageImpl<>(mockSales);

        when(salesRepository.findBySoldAtBetween(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(mockPage);

        when(gitHubModelsService.generateSummary(eq(48), eq(116.02), eq("OREO_DOUBLE"), eq("Miraflores")))
                .thenReturn("OREO_DOUBLE es el más vendido");

        SalesAggregates result = salesService.calculateAggregatesForReport(fromDate, toDate, null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalSales()).isEqualTo(5);
        assertThat(result.getTotalUnits()).isEqualTo(48);
        assertThat(result.getTotalRevenue()).isEqualTo(116.02);
        assertThat(result.getTopSku()).isEqualTo("OREO_DOUBLE");
    }

    /**
     * TEST BONUS: Test con fechas null
     */
    @Test
    @DisplayName("Debe usar última semana cuando las fechas son null")
    void shouldUseLastWeekWhenDatesAreNull() {
        List<Sales> mockSales = List.of(
                createSale("s_001", "OREO_CLASSIC", 10, 1.99, "Miraflores", Instant.now())
        );

        Page<Sales> mockPage = new PageImpl<>(mockSales);

        when(salesRepository.findBySoldAtBetween(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(mockPage);

        when(gitHubModelsService.generateSummary(anyInt(), anyDouble(), anyString(), anyString()))
                .thenReturn("Última semana");

        SalesAggregates result = salesService.calculateAggregatesForReport(null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getFrom()).isNotNull();
        assertThat(result.getTo()).isNotNull();
        assertThat(result.getTo()).isEqualTo(LocalDate.now());
        assertThat(result.getFrom()).isEqualTo(LocalDate.now().minusDays(7));
        assertThat(result.getTotalUnits()).isEqualTo(10);
        assertThat(result.getTotalRevenue()).isEqualTo(19.90);
    }

    // ==========================================
    // MÉTODO AUXILIAR
    // ==========================================

    private Sales createSale(String id, String sku, int units, double price, String branch, Instant soldAt) {
        return Sales.builder()
                .id(id)
                .sku(sku)
                .units(units)
                .price(BigDecimal.valueOf(price))
                .branch(branch)
                .soldAt(soldAt)
                .build();
    }
}
