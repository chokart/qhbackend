package com.suitech.qhbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.embedding.url:https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent}")
    private String embeddingUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Obtiene los embeddings de un texto mediante Gemini Embeddings API.
     * Si la API Key no está configurada, genera un vector sintético determinista para índice local.
     */
    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[128];
        }

        if (hasApiKey()) {
            try {
                String url = embeddingUrl + "?key=" + apiKey.trim();
                
                Map<String, Object> contentMap = new HashMap<>();
                Map<String, Object> partsMap = new HashMap<>();
                partsMap.put("text", text.substring(0, Math.min(text.length(), 2000)));
                contentMap.put("parts", Collections.singletonList(partsMap));

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "models/text-embedding-004");
                requestBody.put("content", contentMap);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode valuesNode = root.path("embedding").path("values");
                    if (valuesNode.isArray()) {
                        float[] embedding = new float[valuesNode.size()];
                        for (int i = 0; i < valuesNode.size(); i++) {
                            embedding[i] = (float) valuesNode.get(i).asDouble();
                        }
                        return embedding;
                    }
                }
            } catch (Exception e) {
                log.warn("Error al llamar a Gemini Embedding API, usando vector local: {}", e.getMessage());
            }
        }

        return generateLocalDeterministicVector(text, 128);
    }

    /**
     * Genera un vector TF-IDF/Hash determinista de dimensión fija para calculo de similitud cuando está offline.
     */
    public float[] generateLocalDeterministicVector(String text, int dim) {
        float[] vector = new float[dim];
        String clean = text.toLowerCase().replaceAll("[^a-z0-9áéíóúñ]", " ");
        String[] words = clean.split("\\s+");
        
        for (String word : words) {
            if (word.length() < 3) continue;
            int hash = Math.abs(word.hashCode());
            int idx = hash % dim;
            vector[idx] += 1.0f;
        }

        // Normalizar L2
        float norm = 0.0f;
        for (float v : vector) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) vector[i] /= norm;
        }
        return vector;
    }

    public double calculateCosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length == 0 || v2.length == 0) return 0.0;
        int minLen = Math.min(v1.length, v2.length);
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < minLen; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
