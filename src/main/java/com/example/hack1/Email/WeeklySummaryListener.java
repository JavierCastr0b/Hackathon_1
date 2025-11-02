package com.example.hack1.Email;

import com.example.hack1.DTO.Request.ReportRequestedEvent;
import com.example.hack1.sales.domain.SalesAggregates;
import com.example.hack1.sales.Service.SalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklySummaryListener {

    private final SalesService salesService;
    private final EmailService emailService;

    @Async
    @EventListener
    public void handleReportRequest(ReportRequestedEvent event) {
        log.info("🔄 Procesando solicitud de reporte: {}", event.getRequestId());

        try {
            log.info("📊 Calculando agregados...");
            SalesAggregates aggregates = salesService.calculateAggregatesForReport(
                    event.getFrom(),
                    event.getTo(),
                    event.getBranch()
            );

            log.info("✅ Resumen generado con IA");

            String emailContent = buildEmailContent(aggregates, event);

            log.info("📧 Enviando email a: {}", event.getEmailTo());
            emailService.sendWeeklySummary(
                    event.getEmailTo(),
                    "📊 Resumen Semanal de Ventas - Oreo",
                    emailContent
            );

            log.info("✅ Reporte completado y enviado: {}", event.getRequestId());

        } catch (Exception e) {
            log.error("❌ Error al procesar reporte {}: {}", event.getRequestId(), e.getMessage());
            sendErrorEmail(event.getEmailTo(), event.getRequestId(), e.getMessage());
        }
    }

    private String buildEmailContent(SalesAggregates aggregates, ReportRequestedEvent event) {
        return String.format("""
            🏢 RESUMEN SEMANAL DE VENTAS - OREO
            ═══════════════════════════════════════════════
            
            📅 Período: %s al %s
            🏪 Sucursal: %s
            
            📊 MÉTRICAS CLAVE
            ═══════════════════════════════════════════════
            • Total de ventas: %d transacciones
            • Unidades vendidas: %,d unidades
            • Ingresos totales: $%,.2f
            • Producto más vendido: %s
            • Sucursal líder: %s
            
            🤖 ANÁLISIS GENERADO POR IA
            ═══════════════════════════════════════════════
            %s
            
            ═══════════════════════════════════════════════
            Solicitado por: %s
            ID de solicitud: %s
            
            Este es un correo automático. Por favor no responder.
            """,
                aggregates.getFrom(),
                aggregates.getTo(),
                aggregates.getBranch() != null ? aggregates.getBranch() : "Todas",
                aggregates.getTotalSales(),
                aggregates.getTotalUnits(),
                aggregates.getTotalRevenue(),
                aggregates.getTopSku(),
                aggregates.getTopBranch(),
                aggregates.getSummary(),
                event.getRequestedBy(),
                event.getRequestId()
        );
    }

    private void sendErrorEmail(String to, String requestId, String errorMessage) {
        try {
            String errorContent = String.format("""
                ❌ ERROR AL GENERAR RESUMEN SEMANAL
                
                Lo sentimos, hubo un error al generar el resumen semanal solicitado.
                
                ID de solicitud: %s
                Error: %s
                
                Por favor, intente nuevamente o contacte al administrador.
                """, requestId, errorMessage);

            emailService.sendWeeklySummary(to, "❌ Error - Resumen Semanal", errorContent);
        } catch (Exception e) {
            log.error("❌ No se pudo enviar email de error: {}", e.getMessage());
        }
    }
}
