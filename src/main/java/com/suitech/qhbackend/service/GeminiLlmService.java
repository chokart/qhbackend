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

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiLlmService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String getCleanApiKey() {
        if (apiKey == null) return "";
        return apiKey.replaceAll("[\"']", "").trim();
    }

    public boolean isLlmConfigured() {
        String clean = getCleanApiKey();
        return !clean.isEmpty();
    }

    public String getProviderName() {
        return "Google Gemini (gemini-3.6-flash)";
    }

    private List<String> getCandidateEndpoints() {
        return Arrays.asList(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent",
                apiUrl,
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash:generateContent",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        );
    }

    public String generateAnswer(String query, List<SourceCitation> sources) {
        return generateAnswer(query, sources, null);
    }

    /**
     * Consulta al LLM Google Gemini enviando la pregunta, el historial de conversación y las fuentes ISO 45001.
     */
    public String generateAnswer(String query, List<SourceCitation> sources, List<com.suitech.qhbackend.dto.AssistantChatRequest.HistoryItem> history) {
        String cleanKey = getCleanApiKey();
        if (cleanKey.isEmpty()) {
            log.info("Consulta RAG recibida pero no hay GEMINI_API_KEY configurada. Servicio LLM no disponible.");
            return null;
        }

        try {
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("ROL Y OBJETIVO:\n");
            contextBuilder.append("Eres el Consultor Senior de Seguridad y Salud en el Trabajo (SST ISO 45001) para la operación industrial y minera QH Relavera.\n");
            contextBuilder.append("Tu responsabilidad es brindar respuestas estrictas, de alta precisión técnica y CERO ALUCINACIONES basadas exclusivamente en la documentación oficial provista.\n\n");

            if (history != null && !history.isEmpty()) {
                contextBuilder.append("--- HISTORIAL DE CONVERSACIÓN RECIENTE DE ESTA SESIÓN ---\n");
                for (com.suitech.qhbackend.dto.AssistantChatRequest.HistoryItem item : history) {
                    if (item.getText() != null && !item.getText().trim().isEmpty()) {
                        String role = "user".equalsIgnoreCase(item.getSender()) ? "Usuario" : "Asistente";
                        contextBuilder.append(role).append(": ").append(item.getText().trim()).append("\n");
                    }
                }
                contextBuilder.append("\n");
            }
            
            contextBuilder.append("--- DOCUMENTOS OFICIALES Y FUENTES DE CONOCIMIENTO EXTRAÍDAS ---\n");
            for (int i = 0; i < sources.size(); i++) {
                SourceCitation src = sources.get(i);
                contextBuilder.append(String.format("FUENTES [%d]:\n", i + 1));
                contextBuilder.append("Documento: ").append(src.getDocumentName()).append("\n");
                if (src.getDocumentCode() != null) contextBuilder.append("Código Oficial: ").append(src.getDocumentCode()).append("\n");
                if (src.getRevision() != null) contextBuilder.append("Revisión/Versión: ").append(src.getRevision()).append("\n");
                if (src.getArea() != null) contextBuilder.append("Área Operativa: ").append(src.getArea()).append("\n");
                if (src.getSection() != null) contextBuilder.append("Sección: ").append(src.getSection()).append("\n");
                if (src.getSectionTitle() != null) contextBuilder.append("Título Sección: ").append(src.getSectionTitle()).append("\n");
                if (src.getPageNumber() != null) contextBuilder.append("Página: ").append(src.getPageNumber()).append("\n");
                contextBuilder.append("Contenido de la Fuente:\n").append(src.getExcerpt()).append("\n----------------------------------------\n");
            }

            contextBuilder.append("\nPREGUNTA DEL USUARIO: ").append(query).append("\n\n");
            contextBuilder.append("INSTRUCCIONES OBLIGATORIAS DE SEGURIDAD SST:\n");
            contextBuilder.append("1. **Basado Estrictamente en Fuentes**: Responde ÚNICAMENTE con la información provista en las Fuentes arriba indexadas. Está estrictamente PROHIBIDO inventar pasos de procedimientos, EPPs, responsabilidades o controles de seguridad.\n");
            contextBuilder.append("2. **Información No Encontrada**: Si la consulta no puede responderse con el contexto provisto, responde exactamente: 'La información solicitada no se encuentra explícitamente detallada en los documentos oficiales indexados de la operación.'\n");
            contextBuilder.append("3. **Citas Normativas Precisas**: Al mencionar cualquier instrucción, EPP o control, cita la fuente indicando el Código del Documento, Revisión, Sección y Página (ejemplo: `[PETS-021 | Rev. 03 | § 4.2 | Pág. 3]`).\n");
            contextBuilder.append("4. **Estructura Clara**: Utiliza formato Markdown profesional (títulos, listas numeradas para pasos de PETS, listas con viñetas para EPPs y controles de IPERC).\n");

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", contextBuilder.toString());

            Map<String, Object> contentObj = new HashMap<>();
            contentObj.put("parts", Collections.singletonList(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(contentObj));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.1);
            generationConfig.put("maxOutputTokens", 4000);
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", cleanKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            for (String targetUrl : getCandidateEndpoints()) {
                try {
                    String fullUrl = targetUrl.contains("?") ? targetUrl + "&key=" + cleanKey : targetUrl + "?key=" + cleanKey;
                    log.info("Consultando endpoint Gemini API: {}", targetUrl);
                    
                    ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.POST, entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        JsonNode root = objectMapper.readTree(response.getBody());
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isArray() && candidates.size() > 0) {
                            JsonNode parts = candidates.get(0).path("content").path("parts");
                            if (parts.isArray()) {
                                StringBuilder fullText = new StringBuilder();
                                for (JsonNode part : parts) {
                                    JsonNode textNode = part.path("text");
                                    if (!textNode.isMissingNode() && textNode.isTextual()) {
                                        fullText.append(textNode.asText());
                                    }
                                }
                                if (fullText.length() > 0) {
                                    return fullText.toString();
                                }
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
     * Método de diagnóstico para probar directamente la API Key contra la API de Gemini probando modelos alternativos.
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

        Map<String, Object> textPart = Collections.singletonMap("text", "Responde únicamente la palabra: OK");
        Map<String, Object> contentObj = Collections.singletonMap("parts", Collections.singletonList(textPart));
        Map<String, Object> requestBody = Collections.singletonMap("contents", Collections.singletonList(contentObj));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", cleanKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        List<Map<String, Object>> attempts = new ArrayList<>();

        for (String testUrlBase : getCandidateEndpoints()) {
            Map<String, Object> attemptLog = new HashMap<>();
            attemptLog.put("endpoint", testUrlBase);
            String testUrl = testUrlBase.contains("?") ? testUrlBase + "&key=" + cleanKey : testUrlBase + "?key=" + cleanKey;

            try {
                ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.POST, entity, String.class);
                attemptLog.put("httpStatus", response.getStatusCode().toString());
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    attemptLog.put("success", true);
                    result.put("success", true);
                    result.put("workingEndpoint", testUrlBase);
                    result.put("httpStatus", response.getStatusCode().toString());
                    result.put("rawResponseBody", response.getBody());
                    attempts.add(attemptLog);
                    result.put("attempts", attempts);
                    return result;
                }
            } catch (HttpStatusCodeException httpEx) {
                attemptLog.put("success", false);
                attemptLog.put("httpStatus", httpEx.getStatusCode().toString());
                attemptLog.put("errorResponse", httpEx.getResponseBodyAsString());
            } catch (Exception e) {
                attemptLog.put("success", false);
                attemptLog.put("exceptionMessage", e.getMessage());
            }
            attempts.add(attemptLog);
        }

        result.put("success", false);
        result.put("message", "Ninguno de los modelos de Gemini respondió con éxito.");
        result.put("attempts", attempts);

        return result;
    }
}
