package com.example.hack1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat; // Para Asserts más legibles
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para el servicio de agregación de ventas.
 * Cumple con los 5 test cases requeridos por el Hackathon.
 */
@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock // 1. Creamos un SIMULADOR (Mock) de la base de datos
    private SalesRepository salesRepository;

    @InjectMocks // 2. Inyectamos ese simulador en la clase que SÍ queremos probar
    private SalesAggregationService salesAggregationService;

    // Fechas estándar para las pruebas
    private static final LocalDate START_DATE = LocalDate.parse("2025-09-01");
    private static final LocalDate END_DATE = LocalDate.parse("2025-09-07");

    // Lista de datos de prueba (los "Seeds" oficiales del hackathon)
    private List<Sale> allMockSales;

    @BeforeEach
    void setUp() {
        // GIVEN: Preparamos los datos de prueba oficiales antes de CADA test
        allMockSales = List.of(
                createSale("s1", "OREO_CLASSIC_12", 25, 1.99, "Miraflores", "2025-09-01T10:30:00Z"),
                createSale("s2", "OREO_DOUBLE", 40, 2.49, "Miraflores", "2025-09-02T15:10:00Z"),
                createSale("s3", "OREO_THINS", 32, 2.19, "San Isidro", "2025-09-03T11:05:00Z"),
                createSale("s4", "OREO_DOUBLE", 55, 2.49, "San Isidro", "2025-09-04T18:50:00Z"),
                createSale("s5", "OREO_CLASSIC_12", 20, 1.99, "Miraflores", "2025-09-05T09:40:00Z")
        );
    }

    // --- INICIO DE TESTS REQUERIDOS ---

    /**
     * Test 1: (REQUERIDO) Test de agregados con datos válidos (Global)
     * Verifica los cálculos para un rol CENTRAL (branch = null) usando el dataset conocido.
     */
    @Test
    @DisplayName("1. Debe calcular agregados globales correctos con datos válidos")
    void shouldCalculateCorrectGlobalAggregatesWithValidData() {
        // GIVEN: Le decimos al Mock que devuelva la lista completa cuando le pregunten
        when(salesRepository.findAllBySoldAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(allMockSales);

        // WHEN: Ejecutamos el método para un rol CENTRAL (branch = null)
        SalesAggregates result = salesAggregationService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN: Verificamos que los cálculos sean correctos (basado en los Seeds)
        // Unidades: 25 + 40 + 32 + 55 + 20 = 172
        // Revenue: (25*1.99) + (40*2.49) + (32*2.19) + (55*2.49) + (20*1.99) = 396.18
        // Top SKU (Unidades): OREO_DOUBLE (40+55=95)
        // Top Branch (Unidades): San Isidro (32+55=87)

        assertThat(result).isNotNull();
        assertThat(result.totalUnits()).isEqualTo(172);
        assertThat(result.totalRevenue()).isEqualTo(396.18);
        assertThat(result.topSku()).isEqualTo("OREO_DOUBLE");
        assertThat(result.topBranch()).isEqualTo("San Isidro");
        assertThat(result.totalSales()).isEqualTo(5);
    }

    /**
     * Test 2: (REQUERIDO) Test con lista vacía
     * Verifica el comportamiento cuando no hay ventas en el rango de fechas.
     */
    @Test
    @DisplayName("2. Debe devolver agregados vacíos (0 y N/A) si no hay ventas")
    void shouldReturnEmptyAggregatesWhenNoSalesFound() {
        // GIVEN: Le decimos al Mock que devuelva una lista vacía
        when(salesRepository.findAllBySoldAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // WHEN: Ejecutamos el método
        SalesAggregates result = salesAggregationService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN: Verificamos que devuelva valores por defecto (0 o "N/A")
        assertThat(result).isNotNull();
        assertThat(result.totalUnits()).isZero();
        assertThat(result.totalRevenue()).isZero();
        assertThat(result.topSku()).isEqualTo("N/A");
        assertThat(result.topBranch()).isEqualTo("N/A");
        assertThat(result.totalSales()).isZero();
    }

    /**
     * Test 3: (REQUERIDO) Test de filtrado por sucursal
     * Verifica el cálculo para un rol BRANCH (branch = "Miraflores")
     */
    @Test
    @DisplayName("3. Debe calcular agregados correctos filtrando por sucursal (BRANCH)")
    void shouldCalculateCorrectAggregatesWithBranchFilter() {
        // GIVEN: Filtramos la lista para simular la consulta de BD por branch
        String branch = "Miraflores";
        List<Sale> mirafloresSales = allMockSales.stream()
                .filter(sale -> branch.equals(sale.getBranch()))
                .toList(); // s1, s2, s5

        // Le decimos al Mock que devuelva ESTA lista filtrada
        when(salesRepository.findAllBySoldAtBetweenAndBranch(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(branch)
        )).thenReturn(mirafloresSales);

        // WHEN: Ejecutamos el método, esta vez pasando la sucursal
        SalesAggregates result = salesAggregationService.calculateAggregates(START_DATE, END_DATE, branch);

        // THEN: Verificamos los cálculos SOLO para Miraflores
        // Unidades: 25 + 40 + 20 = 85
        // Revenue: (25*1.99) + (40*2.49) + (20*1.99) = 189.15
        // Top SKU (Miraflores): OREO_CLASSIC_12 (25+20=45)

        assertThat(result).isNotNull();
        assertThat(result.totalUnits()).isEqualTo(85);
        assertThat(result.totalRevenue()).isEqualTo(189.15);
        assertThat(result.topSku()).isEqualTo("OREO_CLASSIC_12");
        assertThat(result.topBranch()).isEqualTo(branch); // El servicio debe setear esto
        assertThat(result.totalSales()).isEqualTo(3);
    }

    /**
     * Test 4: (REQUERIDO) Test de filtrado por fechas
     * Verifica que el servicio convierta las fechas (LocalDate) a rangos
     * de tiempo (LocalDateTime) correctos al llamar al repositorio.
     */
    @Test
    @DisplayName("4. Debe llamar al repositorio con el rango de fechas (LocalDateTime) correcto")
    void shouldCallRepositoryWithCorrectDateRange() {
        // GIVEN: Fechas de inicio y fin
        LocalDate from = LocalDate.parse("2025-09-02");
        LocalDate to = LocalDate.parse("2025-09-04");

        // Definimos las horas de inicio (00:00) y fin (23:59:59)
        LocalDateTime expectedStart = from.atStartOfDay(); // 2025-09-02T00:00:00
        LocalDateTime expectedEnd = to.atTime(LocalTime.MAX);   // 2025-09-04T23:59:59.999...

        // WHEN: Ejecutamos el método
        salesAggregationService.calculateAggregates(from, to, null);

        // THEN: Verificamos que el service haya llamado al repository
        // EXACTAMENTE con las fechas convertidas.
        // Esto prueba que la lógica de conversión de fechas es correcta.
        verify(salesRepository).findAllBySoldAtBetween(eq(expectedStart), eq(expectedEnd));
    }

    /**
     * Test 5: (REQUERIDO) Test de cálculo de SKU top con empates
     * Verifica que identifique correctamente el SKU más vendido cuando hay empates.
     */
    @Test
    @DisplayName("5. Debe manejar empates en Top SKU correctamente")
    void shouldCorrectlyIdentifyTopSkuWithTies() {
        // GIVEN: Datos personalizados con empate en 20 unidades
        List<Sale> tieSales = List.of(
                createSale("s1", "OREO_A", 20, 1.0, "A", "2025-09-01T10:00:00Z"),
                createSale("s2", "OREO_B", 20, 1.0, "A", "2025-09-01T11:00:00Z"),
                createSale("s3", "OREO_C", 10, 1.0, "A", "2025-09-01T12:00:00Z")
        );
        // Empate: OREO_A y OREO_B tienen 20 unidades

        when(salesRepository.findAllBySoldAtBetween(any(), any())).thenReturn(tieSales);

        // WHEN: Ejecutamos el método
        SalesAggregates result = salesAggregationService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN: Verificamos que el Top SKU sea uno de los empatados
        // (La lógica de Map/reduce usualmente toma el primero que encuentra)
        assertThat(result.topSku()).isIn("OREO_A", "OREO_B");
        assertThat(result.totalUnits()).isEqualTo(50);
    }

    // --- FIN DE TESTS REQUERIDOS ---


    // --- Método Helper para crear datos de prueba ---

    private Sale createSale(String id, String sku, int units, double price, String branch, String soldAt) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setSku(sku);
        sale.setUnits(units);
        sale.setPrice(price);
        sale.setBranch(branch);
        sale.setSoldAt(LocalDateTime.parse(soldAt.replace("Z", ""))); // Parse simple
        return sale;
    }
}