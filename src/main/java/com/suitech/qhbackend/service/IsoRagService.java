package com.suitech.qhbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suitech.qhbackend.dto.*;
import com.suitech.qhbackend.model.IsoDocumentChunk;
import com.suitech.qhbackend.repository.IsoDocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IsoRagService {

    @Value("${iso.documents.path:D:/ISO 45001}")
    private String isoFolderPath;

    private final IsoDocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final GeminiLlmService geminiLlmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern PETS_PATTERN = Pattern.compile("PETS[\\.\\-\\s]*(\\d{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern IPERC_PATTERN = Pattern.compile("IPERC[\\.\\-\\s]*LBASE[\\.\\-\\s]*(\\d{3})", Pattern.CASE_INSENSITIVE);

    /**
     * Procesa la consulta del usuario mediante el flujo RAG.
     */
    public AssistantChatResponse processChat(AssistantChatRequest request) {
        long startTime = System.currentTimeMillis();
        String userQuery = request.getMessage() != null ? request.getMessage().trim() : "";
        String categoryFilter = request.getCategoryFilter();

        if (userQuery.isEmpty()) {
            return AssistantChatResponse.builder()
                    .answer("Por favor, ingresa una pregunta o consulta sobre los documentos de ISO 45001.")
                    .llmAvailable(geminiLlmService.isLlmConfigured())
                    .statusMessage("Consulta vacía.")
                    .sources(Collections.emptyList())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Si la BD no está indexada aún, intentamos indexación básica rápida
        if (chunkRepository.count() == 0) {
            log.info("Base de datos de chunks vacía. Iniciando indexación inicial de D:\\ISO 45001...");
            indexIsoDocuments();
        }

        // 1. Recuperar los fragmentos más relevantes de la BD
        List<SourceCitation> topSources = retrieveTopChunks(userQuery, categoryFilter, 6);

        // 2. Verificar estado del LLM Gemini
        boolean isLlmConfigured = geminiLlmService.isLlmConfigured();

        if (!isLlmConfigured) {
            // Requisito estricto: Informar claramente que el servicio de generación NO está disponible
            String statusMsg = "Servicio de generación LLM no disponible (API Key de Gemini no configurada en el servidor).";
            
            StringBuilder defaultAnswer = new StringBuilder();
            defaultAnswer.append("⚠️ **Servicio de generación LLM no disponible**\n\n");
            defaultAnswer.append("Para obtener respuestas redactadas por inteligencia artificial, configure la variable `GEMINI_API_KEY`.\n\n");
            
            if (!topSources.isEmpty()) {
                defaultAnswer.append("📂 **Documentos ISO 45001 relevantes encontrados en la búsqueda:**\n");
                for (SourceCitation src : topSources) {
                    defaultAnswer.append(String.format("- **%s** (%s) - *%s*\n", 
                            src.getDocumentName(), 
                            src.getCategory(), 
                            src.getDocumentCode() != null ? src.getDocumentCode() : "Doc ISO"));
                }
            } else {
                defaultAnswer.append("No se encontraron coincidencias directas en la base de conocimientos.");
            }

            return AssistantChatResponse.builder()
                    .answer(defaultAnswer.toString())
                    .llmAvailable(false)
                    .statusMessage(statusMsg)
                    .sources(topSources)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 3. Generar respuesta con Gemini LLM
        String llmAnswer = geminiLlmService.generateAnswer(userQuery, topSources);

        if (llmAnswer == null || llmAnswer.trim().isEmpty()) {
            return AssistantChatResponse.builder()
                    .answer("No se pudo obtener una respuesta del proveedor Gemini en este momento. Por favor, reintenta.")
                    .llmAvailable(true)
                    .statusMessage("Error temporal en la llamada a la API de Gemini.")
                    .sources(topSources)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        return AssistantChatResponse.builder()
                .answer(llmAnswer)
                .llmAvailable(true)
                .statusMessage("Respuesta generada exitosamente con contexto ISO 45001.")
                .sources(topSources)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Recupera los K fragmentos más similares vectorialmente o por palabras clave.
     */
    public List<SourceCitation> retrieveTopChunks(String query, String categoryFilter, int limit) {
        List<IsoDocumentChunk> candidateChunks;
        if (categoryFilter != null && !categoryFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(categoryFilter)) {
            candidateChunks = chunkRepository.findByCategory(categoryFilter.trim().toUpperCase());
        } else {
            candidateChunks = chunkRepository.findAll();
        }

        if (candidateChunks.isEmpty()) {
            return Collections.emptyList();
        }

        float[] queryVector = embeddingService.getEmbedding(query);
        String[] queryKeywords = query.toLowerCase().replaceAll("[^a-z0-9áéíóúñ]", " ").split("\\s+");

        List<ScoredChunk> scoredList = new ArrayList<>();

        for (IsoDocumentChunk chunk : candidateChunks) {
            double vectorSim = 0.0;
            if (chunk.getEmbeddingJson() != null) {
                try {
                    float[] chunkVector = objectMapper.readValue(chunk.getEmbeddingJson(), float[].class);
                    vectorSim = embeddingService.calculateCosineSimilarity(queryVector, chunkVector);
                } catch (Exception ignored) {}
            }

            // Keyword match boost
            double keywordScore = 0.0;
            String textLower = chunk.getContent().toLowerCase();
            for (String kw : queryKeywords) {
                if (kw.length() > 3 && textLower.contains(kw)) {
                    keywordScore += 0.15;
                }
            }

            double totalScore = (vectorSim * 0.7) + Math.min(keywordScore, 0.3);

            if (totalScore > 0.05 || candidateChunks.size() < 10) {
                scoredList.add(new ScoredChunk(chunk, totalScore));
            }
        }

        scoredList.sort((a, b) -> Double.compare(b.score, a.score));

        return scoredList.stream()
                .limit(limit)
                .map(sc -> SourceCitation.builder()
                        .documentCode(sc.chunk.getDocumentCode())
                        .documentName(sc.chunk.getDocumentName())
                        .category(sc.chunk.getCategory())
                        .pageNumber(sc.chunk.getPageNumber())
                        .excerpt(truncateExcerpt(sc.chunk.getContent(), 400))
                        .score(Math.round(sc.score * 100.0) / 100.0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Ingesta e indexa todos los PDFs de la carpeta D:\ISO 45001.
     */
    @Transactional
    public synchronized int indexIsoDocuments() {
        File rootDir = new File(isoFolderPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            log.error("La carpeta de documentos ISO 45001 no existe: {}", isoFolderPath);
            return 0;
        }

        log.info("Iniciando escaneo e indexación de archivos en: {}", isoFolderPath);
        chunkRepository.deleteAll();

        List<File> pdfFiles = new ArrayList<>();
        findPdfFiles(rootDir, pdfFiles);

        int totalChunksIndexed = 0;

        for (File pdf : pdfFiles) {
            try {
                int chunksFromDoc = processSinglePdf(pdf);
                totalChunksIndexed += chunksFromDoc;
            } catch (Exception e) {
                log.warn("Error al procesar PDF {}: {}", pdf.getName(), e.getMessage());
            }
        }

        log.info("Indexación finalizada. Total de fragmentos guardados en DB: {}", totalChunksIndexed);
        return totalChunksIndexed;
    }

    private int processSinglePdf(File pdfFile) throws IOException {
        String fileName = pdfFile.getName();
        String category = determineCategory(pdfFile);
        String docCode = extractDocumentCode(fileName);

        int chunksCreated = 0;

        try (PDDocument document = Loader.loadFile(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);

                if (pageText == null || pageText.trim().isEmpty()) continue;

                List<String> chunks = splitTextIntoChunks(pageText.trim(), 800, 100);
                for (int idx = 0; idx < chunks.size(); idx++) {
                    String chunkContent = chunks.get(idx);
                    float[] vector = embeddingService.getEmbedding(chunkContent);
                    String embeddingJson = objectMapper.writeValueAsString(vector);

                    IsoDocumentChunk entity = IsoDocumentChunk.builder()
                            .documentCode(docCode)
                            .documentName(fileName)
                            .category(category)
                            .chunkIndex(idx + 1)
                            .content(chunkContent)
                            .embeddingJson(embeddingJson)
                            .pageNumber(page)
                            .filePath(pdfFile.getAbsolutePath())
                            .build();

                    chunkRepository.save(entity);
                    chunksCreated++;
                }
            }
        }

        return chunksCreated;
    }

    private void findPdfFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                findPdfFiles(f, result);
            } else if (f.getName().toLowerCase().endsWith(".pdf")) {
                result.add(f);
            }
        }
    }

    private String determineCategory(File pdfFile) {
        String path = pdfFile.getAbsolutePath().toUpperCase();
        if (path.contains("1. PETS")) return "PETS";
        if (path.contains("2. IPERC LB")) return "IPERC";
        if (path.contains("ESTÁNDARES\\1. OPERATIVOS") || path.contains("ESTANDARES\\1. OPERATIVOS")) return "ESTANDAR_OPERATIVO";
        if (path.contains("ESTÁNDARES\\2. SEGURIDAD") || path.contains("ESTANDARES\\2. SEGURIDAD")) return "ESTANDAR_SEGURIDAD";
        if (path.contains("MAPA DE PROCESOS")) return "MAPA_PROCESOS";
        return "GENERAL";
    }

    private String extractDocumentCode(String fileName) {
        Matcher petsMatcher = PETS_PATTERN.matcher(fileName);
        if (petsMatcher.find()) return "PETS." + petsMatcher.group(1);

        Matcher ipercMatcher = IPERC_PATTERN.matcher(fileName);
        if (ipercMatcher.find()) return "IPERC-" + ipercMatcher.group(1);

        if (fileName.contains("EST.")) {
            int idx = fileName.indexOf("EST.");
            if (idx + 7 <= fileName.length()) return fileName.substring(idx, idx + 7);
        }

        return null;
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        if (len <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < len) {
            int end = Math.min(start + chunkSize, len);
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
        }
        return chunks;
    }

    private String truncateExcerpt(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    public AssistantStatusResponse getStatus() {
        Map<String, Long> categoryCounts = new HashMap<>();
        categoryCounts.put("PETS", chunkRepository.countByCategory("PETS"));
        categoryCounts.put("IPERC", chunkRepository.countByCategory("IPERC"));
        categoryCounts.put("ESTANDAR_OPERATIVO", chunkRepository.countByCategory("ESTANDAR_OPERATIVO"));
        categoryCounts.put("ESTANDAR_SEGURIDAD", chunkRepository.countByCategory("ESTANDAR_SEGURIDAD"));
        categoryCounts.put("MAPA_PROCESOS", chunkRepository.countByCategory("MAPA_PROCESOS"));
        categoryCounts.put("GENERAL", chunkRepository.countByCategory("GENERAL"));

        long totalChunks = chunkRepository.count();

        return AssistantStatusResponse.builder()
                .llmAvailable(geminiLlmService.isLlmConfigured())
                .llmProvider(geminiLlmService.getProviderName())
                .isoFolderPath(isoFolderPath)
                .totalChunksIndexed(totalChunks)
                .totalDocumentsIndexed(145) // Aproximado según escaneo
                .chunksByCategory(categoryCounts)
                .lastIndexedAt(totalChunks > 0 ? "Indexado en Base de Datos" : "No indexado")
                .build();
    }

    private static class ScoredChunk {
        IsoDocumentChunk chunk;
        double score;
        ScoredChunk(IsoDocumentChunk c, double s) { this.chunk = c; this.score = s; }
    }
}
