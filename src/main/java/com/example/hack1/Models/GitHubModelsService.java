package com.example.hack1.Models;

import com.example.hack1.Exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GitHubModelsService {

    @Value("${github.token}")
    private String token;

    @Value("${github.models.endpoint}")
    private String endpoint;

    @Value("${github.model.id}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateSummary(int totalUnits, double totalRevenue, String topSku, String topBranch) {

        String userPrompt = String.format(
                "Con estos datos: totalUnits=%d, totalRevenue=%.2f, topSku=%s, topBranch=%s. " +
                        "Devuelve un resumen ≤120 palabras para enviar por email.",
                totalUnits, totalRevenue, topSku, topBranch
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "Eres un analista que escribe resúmenes breves y claros para emails corporativos."),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("max_tokens", 200);
        body.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("extra-parameters", "pass-through");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            String url = endpoint + "/chat/completions";
            log.info("Llamando a GitHub Models: {} con modelo: {}", url, model);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    log.info("✅ Resumen generado exitosamente con IA");
                    return content;
                }
            }

            log.warn("⚠️ Respuesta inesperada de GitHub Models, usando fallback");
            return generateFallbackSummary(totalUnits, totalRevenue, topSku, topBranch);

        } catch (Exception e) {
            log.error("❌ Error al llamar a GitHub Models: {}", e.getMessage());
            log.info("📝 Usando resumen generado localmente como fallback");
            return generateFallbackSummary(totalUnits, totalRevenue, topSku, topBranch);
        }
    }

    private String generateFallbackSummary(int totalUnits, double totalRevenue, String topSku, String topBranch) {
        return String.format(
                "Durante el período analizado se vendieron %,d unidades, generando ingresos totales de $%,.2f. " +
                        "El producto más vendido fue '%s', destacándose como el favorito de los clientes. " +
                        "La sucursal '%s' lideró en desempeño comercial. " +
                        "Los indicadores demuestran un crecimiento sostenido y oportunidades de expansión.",
                totalUnits, totalRevenue, topSku, topBranch
        );
    }
}