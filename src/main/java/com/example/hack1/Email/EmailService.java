package com.example.hack1.Email;

import com.example.hack1.DTO.Request.ReportRequestedEvent;
import com.example.hack1.sales.domain.SalesAggregates;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendWeeklySummary(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@oreo.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
        log.info("✅ Email enviado exitosamente a: {}", to);
    }

    public void sendPremiumWeeklySummary(SalesAggregates aggregates, ReportRequestedEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@oreo.com");
            helper.setTo(event.getEmailTo());
            helper.setSubject("🍪 Reporte Semanal Premium - Oreo");

            String htmlContent = buildPremiumHtmlContent(aggregates, event);
            helper.setText(htmlContent, true);

            // TODO: Agregar PDF si attachPdf está habilitado
            if (event.isAttachPdf()) {
                log.info("📎 Generando PDF adjunto...");
                // byte[] pdfBytes = generatePdf(aggregates, event);
                // helper.addAttachment("reporte_oreo.pdf", new ByteArrayResource(pdfBytes));
                log.info("⚠️  Funcionalidad PDF aún no implementada");
            }

            mailSender.send(message);
            log.info("✅ Email premium enviado exitosamente a: {}", event.getEmailTo());

        } catch (MessagingException e) {
            log.error("❌ Error enviando email premium: {}", e.getMessage());
            throw new RuntimeException("Error enviando email premium", e);
        } catch (Exception e) {
            log.error("❌ Error generando contenido premium: {}", e.getMessage());
            throw new RuntimeException("Error generando contenido premium", e);
        }
    }

    private String buildPremiumHtmlContent(SalesAggregates aggregates, ReportRequestedEvent event) {
        StringBuilder html = new StringBuilder();

        // Header con CSS
        html.append("<!DOCTYPE html>")
                .append("<html><head>")
                .append("<meta charset=\"UTF-8\">")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }")
                .append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; text-align: center; }")
                .append(".header h1 { margin: 0; font-size: 28px; }")
                .append(".container { max-width: 800px; margin: 20px auto; }")
                .append(".metric { background: #f8f9fa; border-left: 4px solid #667eea; padding: 20px; margin: 15px 0; border-radius: 5px; }")
                .append(".metric h3 { margin: 0 0 10px 0; color: #667eea; }")
                .append(".metric-value { font-size: 32px; font-weight: bold; color: #764ba2; }")
                .append(".analysis { background: #fff3cd; border-left: 4px solid #ffc107; padding: 20px; margin: 20px 0; border-radius: 5px; }")
                .append(".footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; }")
                .append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }")
                .append("th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }")
                .append("th { background-color: #667eea; color: white; }")
                .append("tr:hover { background-color: #f5f5f5; }")
                .append("</style>")
                .append("</head><body>");

        // Contenedor principal
        html.append("<div class=\"container\">");

        // Header
        html.append("<div class=\"header\">")
                .append("<h1>🍪 Reporte Semanal Oreo</h1>")
                .append("<p style=\"margin: 10px 0 0 0;\">")
                .append(aggregates.getFrom()).append(" al ").append(aggregates.getTo())
                .append("</p>");

        if (aggregates.getBranch() != null) {
            html.append("<p style=\"margin: 5px 0 0 0;\">🏪 ").append(aggregates.getBranch()).append("</p>");
        }

        html.append("</div>");

        // Métricas principales
        html.append("<h2 style=\"color: #667eea; margin-top: 30px;\">📊 Métricas Clave</h2>");

        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 20px 0;\">");

        html.append("<div class=\"metric\">")
                .append("<h3>💰 Ingresos</h3>")
                .append("<div class=\"metric-value\">$").append(String.format("%.2f", aggregates.getTotalRevenue())).append("</div>")
                .append("</div>");

        html.append("<div class=\"metric\">")
                .append("<h3>📦 Unidades</h3>")
                .append("<div class=\"metric-value\">").append(aggregates.getTotalUnits()).append("</div>")
                .append("</div>");

        html.append("<div class=\"metric\">")
                .append("<h3>🛒 Ventas</h3>")
                .append("<div class=\"metric-value\">").append(aggregates.getTotalSales()).append("</div>")
                .append("</div>");

        html.append("</div>");

        // Gráficos si están habilitados
        if (event.isIncludeCharts()) {
            html.append("<h2 style=\"color: #667eea; margin-top: 30px;\">📈 Visualizaciones</h2>");

            // Gráfico de barras - Top SKU
            html.append("<div style=\"margin: 20px 0;\">")
                    .append("<h3>Top Producto Vendido</h3>")
                    .append("<img src=\"").append(generateChartUrl(aggregates.getTopSku())).append("\" ")
                    .append("alt=\"Top SKU Chart\" style=\"width: 100%; max-width: 600px; border-radius: 8px;\" />")
                    .append("</div>");

            // Gráfico de pie - Distribución por sucursal si aplica
            if (aggregates.getTopBranch() != null) {
                html.append("<div style=\"margin: 20px 0;\">")
                        .append("<h3>Distribución por Sucursal</h3>")
                        .append("<img src=\"").append(generatePieChartUrl(aggregates.getTopBranch())).append("\" ")
                        .append("alt=\"Branch Distribution Chart\" style=\"width: 100%; max-width: 600px; border-radius: 8px;\" />")
                        .append("</div>");
            }
        }

        // Análisis de IA
        html.append("<div class=\"analysis\">")
                .append("<h2 style=\"margin-top: 0; color: #856404;\">🤖 Análisis Generado por IA</h2>")
                .append("<p>").append(aggregates.getSummary()).append("</p>")
                .append("</div>");

        // Footer
        html.append("<div class=\"footer\">")
                .append("<p>Solicitado por: ").append(event.getRequestedBy()).append("</p>")
                .append("<p>ID de solicitud: ").append(event.getRequestId()).append("</p>")
                .append("<p style=\"color: #999;\">Este es un correo automático. Por favor no responder.</p>")
                .append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    private String generateChartUrl(String topSku) {
        // Generar URL para QuickChart.io con bar chart
        try {
            String chartConfig = URLEncoder.encode(
                    String.format(
                            "{type:'bar',data:{labels:['%s'],datasets:[{label:'Unidades Vendidas',data:[%d],backgroundColor:'#667eea'}]}}",
                            topSku, 100
                    ),
                    StandardCharsets.UTF_8
            );
            return "https://quickchart.io/chart?c=" + chartConfig;
        } catch (Exception e) {
            log.error("Error generando chart URL: {}", e.getMessage());
            return "";
        }
    }

    private String generatePieChartUrl(String topBranch) {
        try {
            String chartConfig = URLEncoder.encode(
                    String.format(
                            "{type:'pie',data:{labels:['%s','Otras'],datasets:[{data:[80,20],backgroundColor:['#667eea','#e0e0e0']}]}}",
                            topBranch
                    ),
                    StandardCharsets.UTF_8
            );
            return "https://quickchart.io/chart?c=" + chartConfig;
        } catch (Exception e) {
            log.error("Error generando pie chart URL: {}", e.getMessage());
            return "";
        }
    }
}
