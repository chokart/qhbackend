package com.suitech.qhbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suitech.qhbackend.dto.SourceCitation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class GeminiLlmService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isLlmConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String getProviderName() {
        return "Google Gemini (gemini-1.5-flash)";
    }

    /**
     * Consulta al LLM Google Gemini enviando la pregunta y el contexto de las fuentes ISO 45001 recuperadas.
     * Si no hay API Key configurada, NO genera respuestas sintéticas y retorna null.
     */
    public String generateAnswer(String query, List<SourceCitation> sources) {
        if (!isLlmConfigured()) {
            log.info("Consulta RAG recibida pero no hay GEMINI_API_KEY configurada. Servicio LLM no disponible.");
            return null;
        }

        try {
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("Eres el Asistente Técnico y de Seguridad en Salud Ocupacional (ISO 45001) para la operación QH Relavera.\n");
            contextBuilder.append("Responde a la consulta del usuario de forma profesional, clara y precisa, basada ESTRICTAMENTE en la información de los siguientes documentos de la carpeta D:\\ISO 45001:\n\n");
            
            for (int i = 0; i < sources.size(); i++) {
                SourceCitation src = sources.get(i);
                contextBuilder.append(String.format("--- FUENTE %d ---\n", i + 1));
                contextBuilder.append("Documento: ").append(src.getDocumentName()).append("\n");
                if (src.getDocumentCode() != null) {
                    contextBuilder.append("Código: ").append(src.getDocumentCode()).append("\n");
                }
                contextBuilder.append("Categoría: ").append(src.getCategory()).append("\n");
                if (src.getPageNumber() != null) {
                    contextBuilder.append("Página: ").append(src.getPageNumber()).append("\n");
                }
                contextBuilder.append("Contenido:\n").append(src.getExcerpt()).append("\n\n");
            }

            contextBuilder.append("PREGUNTA DEL USUARIO: ").append(query).append("\n\n");
            contextBuilder.append("INSTRUCCIONES:\n");
            contextBuilder.append("1. Responde de forma estructurada usando formato Markdown (títulos, viñetas, negritas).\n");
            contextBuilder.append("2. Cita explícitamente los códigos de PETS/IPERC o nombres de estándares cuando menciones procedimientos o controles.\n");
            contextBuilder.append("3. Si la información no está presente en las fuentes recuperadas, indícalo claramente.");

            String endpointUrl = apiUrl + "?key=" + apiKey.trim();

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", contextBuilder.toString());

            Map<String, Object> contentObj = new HashMap<>();
            contentObj.put("parts", Collections.singletonList(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(contentObj));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", 1500);
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(endpointUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        return textNode.asText();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al consultar Gemini LLM API: {}", e.getMessage(), e);
        }

        return null;
    }
}
