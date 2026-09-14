package com.suitech.qhbackend.service;

import com.suitech.qhbackend.model.IsoDocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class IsoSemanticPreprocessor {

    private static final Pattern PETS_CODE_PATTERN = Pattern.compile("PETS[\\.\\-\\s]*(\\d{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern IPERC_CODE_PATTERN = Pattern.compile("IPERC[\\.\\-\\s]*LBASE[\\.\\-\\s]*(\\d{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESTANDAR_CODE_PATTERN = Pattern.compile("(EST[\\.\\-\\s]*\\d{3}|GPS[\\.\\-\\s]*ES[\\.\\-\\s]*\\d{2})", Pattern.CASE_INSENSITIVE);

    private static final Pattern STEP_PATTERN = Pattern.compile("(?:PASO|STEP|\\d+\\.)\\s*(\\d+[\\.\\d+]*\\s*[^\\n]+)", Pattern.CASE_INSENSITIVE);

    /**
     * Convierte el texto completo de una página/documento PDF en fragmentos semánticos estructurados de alta densidad.
     */
    public List<IsoDocumentChunk> preprocessDocument(String fileName, String category, int pageNumber, String rawText, String filePath) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String cleanText = cleanRawText(rawText);
        String docCode = extractDocCode(fileName, cleanText);

        List<IsoDocumentChunk> chunks = new ArrayList<>();

        if ("PETS".equalsIgnoreCase(category)) {
            chunks.addAll(processPetsText(fileName, docCode, pageNumber, cleanText, filePath));
        } else if ("IPERC".equalsIgnoreCase(category)) {
            chunks.addAll(processIpercText(fileName, docCode, pageNumber, cleanText, filePath));
        } else if (category.contains("ESTANDAR")) {
            chunks.addAll(processEstandarText(fileName, docCode, category, pageNumber, cleanText, filePath));
        } else if ("LEYES".equalsIgnoreCase(category) || category.contains("LEY") || category.contains("NORMATIVA")) {
            chunks.addAll(processLeyesText(fileName, docCode, category, pageNumber, cleanText, filePath));
        } else {
            chunks.addAll(processGenericText(fileName, docCode, category, pageNumber, cleanText, filePath));
        }

        if (chunks.isEmpty()) {
            chunks.addAll(processGenericText(fileName, docCode, category, pageNumber, cleanText, filePath));
        }

        return chunks;
    }

    private List<IsoDocumentChunk> processPetsText(String fileName, String docCode, int pageNumber, String text, String filePath) {
        List<IsoDocumentChunk> list = new ArrayList<>();

        // Buscar secciones de Pasos de Ejecución
        String[] paragraphs = text.split("\\n{2,}");
        int index = 1;

        StringBuilder currentBlock = new StringBuilder();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (trimmed.length() < 15) continue;

            // Si detectamos un nuevo paso o sección amplia
            if (STEP_PATTERN.matcher(trimmed).find() || currentBlock.length() > 600) {
                if (currentBlock.length() > 50) {
                    list.add(createChunk(docCode, fileName, "PETS", index++, 
                            enrichPetsChunk(docCode, fileName, currentBlock.toString()), pageNumber, filePath));
                }
                currentBlock = new StringBuilder();
            }

            if (currentBlock.length() > 0) currentBlock.append("\n");
            currentBlock.append(trimmed);
        }

        if (currentBlock.length() > 50) {
            list.add(createChunk(docCode, fileName, "PETS", index, 
                    enrichPetsChunk(docCode, fileName, currentBlock.toString()), pageNumber, filePath));
        }

        return list;
    }

    private List<IsoDocumentChunk> processIpercText(String fileName, String docCode, int pageNumber, String text, String filePath) {
        List<IsoDocumentChunk> list = new ArrayList<>();

        // Para IPERC: agrupar por bloques de Peligro / Riesgo / Medida de Control
        String[] lines = text.split("\\n+");
        StringBuilder currentBlock = new StringBuilder();
        int index = 1;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            currentBlock.append(trimmed).append("\n");

            if (currentBlock.length() >= 500 || trimmed.toLowerCase().contains("control") || trimmed.toLowerCase().contains("riesgo")) {
                list.add(createChunk(docCode, fileName, "IPERC", index++, 
                        enrichIpercChunk(docCode, fileName, currentBlock.toString()), pageNumber, filePath));
                currentBlock = new StringBuilder();
            }
        }

        if (currentBlock.length() > 50) {
            list.add(createChunk(docCode, fileName, "IPERC", index, 
                    enrichIpercChunk(docCode, fileName, currentBlock.toString()), pageNumber, filePath));
        }

        return list;
    }

    private List<IsoDocumentChunk> processEstandarText(String fileName, String docCode, String category, int pageNumber, String text, String filePath) {
        List<IsoDocumentChunk> list = new ArrayList<>();
        List<String> rawChunks = splitTextBySentence(text, 600, 100);

        int index = 1;
        for (String raw : rawChunks) {
            String enriched = String.format("📋 ESTÁNDAR DE SEGURIDAD [%s - %s]\nDocumento: %s\n%s", 
                    docCode != null ? docCode : "ESTÁNDAR", category, fileName, raw);
            list.add(createChunk(docCode, fileName, category, index++, enriched, pageNumber, filePath));
        }

        return list;
    }

    private List<IsoDocumentChunk> processLeyesText(String fileName, String docCode, String category, int pageNumber, String text, String filePath) {
        List<IsoDocumentChunk> list = new ArrayList<>();
        List<String> rawChunks = splitTextBySentence(text, 600, 100);

        int index = 1;
        for (String raw : rawChunks) {
            String enriched = String.format("⚖️ MARCO LEGAL Y NORMATIVA DE SEGURIDAD MINERA (D.S. 024-2016-EM)\nDocumento: %s\nPágina: %d\nContenido de la Norma:\n%s", 
                    fileName, pageNumber, raw);
            list.add(createChunk(docCode != null ? docCode : "D.S. 024-2016-EM", fileName, "LEYES", index++, enriched, pageNumber, filePath));
        }

        return list;
    }

    private List<IsoDocumentChunk> processGenericText(String fileName, String docCode, String category, int pageNumber, String text, String filePath) {
        List<IsoDocumentChunk> list = new ArrayList<>();
        List<String> rawChunks = splitTextBySentence(text, 600, 100);

        int index = 1;
        for (String raw : rawChunks) {
            String enriched = String.format("[%s - %s]\nDocumento: %s\n%s", 
                    docCode != null ? docCode : "DOC", category, fileName, raw);
            list.add(createChunk(docCode, fileName, category, index++, enriched, pageNumber, filePath));
        }

        return list;
    }

    private String enrichPetsChunk(String docCode, String fileName, String content) {
        return String.format("📑 PROCEDIMIENTO SEGURO PETS [%s]\nDocumento: %s\nContenido del Procedimiento:\n%s", 
                docCode != null ? docCode : "PETS", fileName, content);
    }

    private String enrichIpercChunk(String docCode, String fileName, String content) {
        return String.format("🛡️ MATRIZ DE RIESGOS IPERC LÍNEA BASE [%s]\nDocumento: %s\nPeligros, Riesgos y Medidas de Control:\n%s", 
                docCode != null ? docCode : "IPERC", fileName, content);
    }

    private static final Pattern REVISION_PATTERN = Pattern.compile("(?:REV|REVISIÓ|REVISION|VERSIÓ|VERSION|REV\\.)[\\s\\.:]*([0-9]{2,3}|[A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_PATTERN = Pattern.compile("(?:^|\\n)(?:§|SECCIÓ|SECCION|ITEM|CAPÍTULO|CAPITULO|\\b(\\d+\\.\\d+(?:\\.\\d+)?))\\s*[:\\-]?\\s*([^\\n]{3,60})", Pattern.CASE_INSENSITIVE);

    private IsoDocumentChunk createChunk(String docCode, String fileName, String category, int chunkIdx, String content, int page, String path) {
        String revision = extractRevision(content, fileName);
        String section = extractSectionNumber(content);
        String sectionTitle = extractSectionTitle(content);
        String docType = category != null ? category.toUpperCase() : "GENERAL";
        if (docType.contains("ESTANDAR")) docType = "ESTANDAR";

        return IsoDocumentChunk.builder()
                .documentCode(docCode)
                .documentName(fileName)
                .category(category)
                .documentType(docType)
                .revision(revision)
                .area("Operaciones QH")
                .section(section)
                .sectionTitle(sectionTitle)
                .status("ACTIVO")
                .chunkIndex(chunkIdx)
                .content(content)
                .pageNumber(page)
                .filePath(path)
                .build();
    }

    private String extractRevision(String text, String fileName) {
        Matcher mText = REVISION_PATTERN.matcher(text);
        if (mText.find()) return mText.group(1).trim();

        Matcher mFile = REVISION_PATTERN.matcher(fileName);
        if (mFile.find()) return mFile.group(1).trim();

        return "01";
    }

    private String extractSectionNumber(String text) {
        Matcher m = SECTION_PATTERN.matcher(text);
        if (m.find()) {
            if (m.group(1) != null) return m.group(1).trim();
            return "§ Sec";
        }
        return "General";
    }

    private String extractSectionTitle(String text) {
        Matcher m = SECTION_PATTERN.matcher(text);
        if (m.find() && m.groupCount() >= 2 && m.group(2) != null) {
            String title = m.group(2).trim();
            if (title.length() > 60) title = title.substring(0, 60);
            return title;
        }
        return "Disposiciones Generales";
    }

    private String cleanRawText(String text) {
        return text.replaceAll("\\r\\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String extractDocCode(String fileName, String text) {
        Matcher petsMatcher = PETS_CODE_PATTERN.matcher(fileName);
        if (petsMatcher.find()) return "PETS." + petsMatcher.group(1);

        Matcher ipercMatcher = IPERC_CODE_PATTERN.matcher(fileName);
        if (ipercMatcher.find()) return "IPERC-" + ipercMatcher.group(1);

        Matcher estMatcher = ESTANDAR_CODE_PATTERN.matcher(fileName);
        if (estMatcher.find()) return estMatcher.group(1).toUpperCase();

        return null;
    }

    private List<String> splitTextBySentence(String text, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        int len = text.length();
        if (len <= chunkSize) {
            result.add(text);
            return result;
        }

        int start = 0;
        while (start < len) {
            int end = Math.min(start + chunkSize, len);
            if (end < len) {
                int lastDot = text.lastIndexOf('.', end);
                if (lastDot > start + 200) {
                    end = lastDot + 1;
                }
            }
            result.add(text.substring(start, end).trim());
            start = Math.max(end - overlap, start + 100);
        }
        return result;
    }
}
