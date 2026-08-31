package com.suitech.qhbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suitech.qhbackend.dto.SourceCitation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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

    public String getCleanApiKey() {
        if (apiKey == null) return "";
        return apiKey.replaceAll("[\"']", "").trim();
    }

    public boolean isLlmConfigured() {
        String clean = getCleanApiKey();
        return !clean.isEmpty();
    }

    public String getProviderName() {
        return "Google Gemini (gemini-1.5-flash)";
    }

    /**
     * Consulta al LLM Google Gemini enviando la pregunta y el contexto de las fuentes ISO 45001 recuperadas.
     */
    public String generateAnswer(String query, List<SourceCitation> sources) {
        String cleanKey = getCleanApiKey();
        if (cleanKey.isEmpty()) {
            log.info("Consulta RAG recibida pero no hay GEMINI_API_KEY configurada. Servicio LLM no disponible.");
            return null;
        }

        try {
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("Eres el Asistente Técnico y de Seguridad en Salud Ocupacional (ISO 45001) para la operación QH Relavera.\n");
            contextBuilder.append("Responde a la consulta del usuario de forma profesional, clara y precisa, basada ESTRICTAMENTE en la información de los siguientes documentos de la carpeta ISO 45001:\n\n");
            
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

            List<String> candidateEndpoints = Arrays.asList(
                    apiUrl,
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent"
            );

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
            headers.set("x-goog-api-key", cleanKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            for (String targetUrl : candidateEndpoints) {
                try {
                    String fullUrl = targetUrl.contains("?") ? targetUrl + "&key=" + cleanKey : targetUrl + "?key=" + cleanKey;
                    log.info("Consultando endpoint Gemini API: {}", targetUrl);
                    
                    ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.POST, entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        JsonNode root = objectMapper.readTree(response.getBody());
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isArray() && candidates.size() > 0) {
                            JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                            if (!textNode.isMissingNode() && !textNode.asText().isEmpty()) {
                                return textNode.asText();
                            }
                        }
                    }
                } catch (HttpStatusCodeException httpEx) {
                    log.warn("Gemini API respondió error HTTP {} en {}: {}", 
                            httpEx.getStatusCode(), targetUrl, httpEx.getResponseBodyAsString());
                } catch (Exception ex) {
                    log.warn("Fallo al conectar con Gemini endpoint {}: {}", targetUrl, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error general en GeminiLlmService: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Método de diagnóstico para probar directamente la API Key contra la API de Gemini.
     */
    public Map<String, Object> testGeminiApiConnection() {
        Map<String, Object> result = new HashMap<>();
        String cleanKey = getCleanApiKey();

        result.put("configured", isLlmConfigured());
        if (!isLlmConfigured()) {
            result.put("success", false);
            result.put("message", "GEMINI_API_KEY no está configurada en las variables de entorno.");
            return result;
        }

        String keyPreview = cleanKey.length() > 8 
                ? cleanKey.substring(0, 4) + "..." + cleanKey.substring(cleanKey.length() - 4) 
                : "KEY_CORTA";
        result.put("keyPreview", keyPreview);

        String testUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + cleanKey;

        Map<String, Object> textPart = Collections.singletonMap("text", "Responde únicamente la palabra: OK");
        Map<String, Object> contentObj = Collections.singletonMap("parts", Collections.singletonList(textPart));
        Map<String, Object> requestBody = Collections.singletonMap("contents", Collections.singletonList(contentObj));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", cleanKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.POST, entity, String.class);
            result.put("httpStatus", response.getStatusCode().toString());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                result.put("success", true);
                result.put("rawResponseBody", response.getBody());
            } else {
                result.put("success", false);
                result.put("rawResponseBody", response.getBody());
            }
        } catch (HttpStatusCodeException httpEx) {
            result.put("success", false);
            result.put("httpStatus", httpEx.getStatusCode().toString());
            result.put("errorResponse", httpEx.getResponseBodyAsString());
            log.error("Diagnóstico Gemini API Error HTTP {}: {}", httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
        } catch (Exception e) {
            result.put("success", false);
            result.put("exceptionMessage", e.getMessage());
            log.error("Diagnóstico Gemini API Excepción: {}", e.getMessage(), e);
        }

        return result;
    }
}
