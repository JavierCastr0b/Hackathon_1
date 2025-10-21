import com.oreo.insight.factory.model.Sale;
import com.oreo.insight.factory.repository.SalesRepository;
import com.oreo.insight.factory.dto.SalesAggregates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat; // Para Asserts más legibles
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock // 1. Creamos un SIMULADOR (Mock) de la base de datos
    private SalesRepository salesRepository;

    @InjectMocks // 2. Inyectamos ese simulador en la clase que SÍ queremos probar
    private SalesAggregationService salesAggregationService;

    // Fechas estándar para las pruebas
    private static final LocalDate START_DATE = LocalDate.parse("2025-09-01");
    private static final LocalDate END_DATE = LocalDate.parse("2025-09-07");

    // Lista de datos de prueba (los "Seeds" del hackathon)
    private List<Sale> mockSales;

    @BeforeEach
    void setUp() {
        // GIVEN: Preparamos los datos de prueba antes de CADA test
        // Usamos los 5 "Seeds" oficiales
        mockSales = List.of(
                createSale("s1", "OREO_CLASSIC_12", 25, 1.99, "Miraflores", "2025-09-01T10:30:00Z"),
                createSale("s2", "OREO_DOUBLE", 40, 2.49, "Miraflores", "2025-09-02T15:10:00Z"),
                createSale("s3", "OREO_THINS", 32, 2.19, "San Isidro", "2025-09-03T11:05:00Z"),
                createSale("s4", "OREO_DOUBLE", 55, 2.49, "San Isidro", "2025-09-04T18:50:00Z"),
                createSale("s5", "OREO_CLASSIC_12", 20, 1.99, "Miraflores", "2025-09-05T09:40:00Z")
        );
    }

    /**
     * Test 1: (REQUERIDO) Test de agregados con datos válidos (Global)
     * Prueba el cálculo para un rol CENTRAL (branch = null)
     */
    @Test
    void shouldCalculateCorrectGlobalAggregatesWithValidData() {
        // GIVEN: Le decimos al Mock qué debe devolver cuando le pregunten
        // Usamos any() porque el service convierte LocalDate a LocalDateTime
        when(salesRepository.findAllBySoldAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockSales);

        // WHEN: Ejecutamos el método que queremos probar
        SalesAggregates result = salesAggregationService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN: Verificamos que los cálculos sean correctos
        // Unidades: 25 + 40 + 32 + 55 + 20 = 172
        // Revenue: (25*1.99) + (40*2.49) + (32*2.19) + (55*2.49) + (20*1.99) = 396.18
        // Top SKU (Unidades): OREO_DOUBLE (40+55=95) vs CLASSIC (25+20=45) vs THINS (32)
        // Top Branch (Unidades): San Isidro (32+55=87) vs Miraflores (25+40+20=85)

        assertThat(result).isNotNull();
        assertThat(result.getTotalUnits()).isEqualTo(172);
        assertThat(result.getTotalRevenue()).isEqualTo(396.18);
        assertThat(result.getTopSku()).isEqualTo("OREO_DOUBLE");
        assertThat(result.getTopBranch()).isEqualTo("San Isidro");
        assertThat(result.getTotalSales()).isEqualTo(5);
    }

    /**
     * Test 2: (REQUERIDO) Test con lista vacía
     * Prueba qué pasa si no hay ventas en el rango.
     */
    @Test
    void shouldReturnEmptyAggregatesWhenNoSalesFound() {
        // GIVEN: Le decimos al Mock que devuelva una lista vacía
        when(salesRepository.findAllBySoldAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // WHEN: Ejecutamos el método
        SalesAggregates result = salesAggregationService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN: Verificamos que devuelva valores por defecto (0 o "N/A")
        assertThat(result).isNotNull();
        assertThat(result.getTotalUnits()).isEqualTo(0);
        assertThat(result.getTotalRevenue()).isEqualTo(0.0);
        assertThat(result.getTopSku()).isEqualTo("N/A");
        assertThat(result.getTopBranch()).isEqualTo("N/A");
        assertThat(result.getTotalSales()).isEqualTo(0);
    }

    /**
     * Test 3: (REQUERIDO) Test de filtrado por sucursal
     * Prueba el cálculo para un rol BRANCH (branch = "Miraflores")
     */
    @Test
    void shouldCalculateCorrectAggregatesWithBranchFilter() {
        // GIVEN: Filtramos la lista para simular la consulta de BD por branch
        List<Sale> mirafloresSales = mockSales.stream()
                .filter(sale -> "Miraflores".equals(sale.getBranch()))
                .toList(); // s1, s2, s5

        // Le decimos al Mock que devuelva ESTA lista filtrada
        when(salesRepository.findAllBySoldAtBetweenAndBranch(
                any(LocalDateTime.class), any(LocalDateTime.class), eq("Miraflores")
        )).thenReturn(mirafloresSales);

        // WHEN: Ejecutamos el método, esta vez pasando la sucursal
        SalesAggregates result = salesAggregationService.calculateAggregates(START_DATE, END_DATE, "Miraflores");

        // THEN: Verificamos los cálculos SOLO para Miraflores
        // Unidades: 25 + 40 + 20 = 85
        // Revenue: (25*1.99) + (40*2.49) + (20*1.99) = 189.15
        // Top SKU (Miraflores): CLASSIC (25+20=45) vs DOUBLE (40)
        // Top Branch: Debe ser "Miraflores" porque estamos filtrando

        assertThat(result).isNotNull();
        assertThat(result.getTotalUnits()).isEqualTo(85);
        assertThat(result.getTotalRevenue()).isEqualTo(189.15);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC_12");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores"); // El servicio debe setear esto
        assertThat(result.getTotalSales()).isEqualTo(3);
    }

    /**
     * Test 4: (REQUERIDO) Test de filtrado por fechas (Verificación)
     * Este test verifica que el servicio llame al repositorio con las fechas correctas.
     */
    @Test
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
        // Esto prueba que la lógica de conversión de fechas del servicio es correcta.
        verify(salesRepository).findAllBySoldAtBetween(eq(expectedStart), eq(expectedEnd));
    }

    /**
     * Test 5: (REQUERIDO) Test de cálculo de SKU top con empates
     * Prueba que se maneje un empate (alfabéticamente o cualquiera está bien)
     */
    @Test
    void shouldCorrectlyIdentifyTopSkuWithTies() {
        // GIVEN: Datos personalizados con empate
        List<Sale> tieSales = List.of(
                createSale("s1", "OREO_A", 10, 1.0, "A", "2025-09-01T10:00:00Z"),
                createSale("s2", "OREO_B", 20, 1.0, "A", "2025-09-01T11:00:00Z"),
                createSale("s3", "OREO_C", 20, 1.0, "A", "2025-09-01T12:00:00Z")
        );
        // Empate: OREO_B y OREO_C tienen 20 unidades

        when(salesRepository.findAllBySoldAtBetween(any(), any())).thenReturn(tieSales);

        // WHEN: Ejecutamos el método
        SalesAggregates result = salesAggregationService.calculateAggregates(START_DATE, END_DATE, null);

        // THEN: Verificamos que el Top SKU sea uno de los empatados
        assertThat(result.getTopSku()).isIn("OREO_B", "OREO_C");
    }


    // --- Método Helper para crear datos de prueba ---

    private Sale createSale(String id, String sku, int units, double price, String branch, String soldAt) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setSku(sku);
        sale.setUnits(units);
        sale.setPrice(price);
        sale.setBranch(branch);
        sale.setSoldAt(LocalDateTime.parse(soldAt.replace("Z", ""))); // Simple parse
        return sale;
    }
}