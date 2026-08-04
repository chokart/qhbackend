package com.suitech.qhbackend.service;

import com.suitech.qhbackend.model.Cancha;
import com.suitech.qhbackend.model.CanchaCapa;
import com.suitech.qhbackend.model.CanchaStatus;
import com.suitech.qhbackend.repository.CanchaCapaRepository;
import com.suitech.qhbackend.repository.CanchaRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeotecniaReportService {

    private final CanchaRepository canchaRepository;
    private final CanchaCapaRepository canchaCapaRepository;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportReportResult {
        private int principalUpdated;
        private int lateralUpdated;
        private List<String> logMessages;
    }

    private enum CurrentSection {
        DIQUE_PRINCIPAL,
        DIQUE_LATERAL
    }

    // 1. Procesar Perfil PDF (Actualiza Niveles / Elevaciones en msnm)
    public ImportReportResult processPerfilPdf(MultipartFile file, String updatedBy) throws IOException {
        return processPerfilBytes(file.getBytes(), updatedBy);
    }

    public ImportReportResult processPerfilBytes(byte[] pdfBytes, String updatedBy) throws IOException {
        List<String> logs = new ArrayList<>();
        int principalCount = 0;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);
            String[] lines = fullText.split("\\r?\\n");

            Pattern canchaTokenPattern = Pattern.compile("^C[-_ ]?(\\d{1,2})$", Pattern.CASE_INSENSITIVE);
            Pattern elevationPattern = Pattern.compile("\\b(1[0-2]\\d{2}\\.\\d{2})\\b");

            Map<Integer, Double> principalHeights = new HashMap<>();
            int[] searchOffsets = {0, 1, -1, 2, -2};

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                Matcher canchaMatcher = canchaTokenPattern.matcher(line);

                if (canchaMatcher.find()) {
                    int canchaNum = Integer.parseInt(canchaMatcher.group(1));
                    if (canchaNum <= 0 || canchaNum > 30) continue;

                    Double foundHeight = null;
                    for (int offset : searchOffsets) {
                        int idx = i + offset;
                        if (idx >= 0 && idx < lines.length) {
                            Matcher elevMatcher = elevationPattern.matcher(lines[idx].trim());
                            if (elevMatcher.find()) {
                                double val = Double.parseDouble(elevMatcher.group(1));
                                if (val >= 1110.0 && val <= 1300.0 && val != 1220.0 && val != 1215.0) {
                                    foundHeight = val;
                                    break;
                                }
                            }
                        }
                    }

                    if (foundHeight != null && !principalHeights.containsKey(canchaNum)) {
                        principalHeights.put(canchaNum, foundHeight);
                    }
                }
            }

            for (Map.Entry<Integer, Double> entry : new TreeMap<>(principalHeights).entrySet()) {
                int num = entry.getKey();
                double height = entry.getValue();

                Cancha cancha = canchaRepository.findByNumber(num)
                        .orElseGet(() -> Cancha.builder().number(num).build());

                cancha.setCurrentHeight(height);
                cancha.setLastUpdatedBy(updatedBy != null ? updatedBy : "PERFIL_GEOTECNIA_PDF");
                canchaRepository.save(cancha);
                principalCount++;

                logs.add(String.format("Dique Principal C-%02d -> Elevación actual: %.2f msnm", num, height));
            }
        }

        if (principalCount == 0) {
            logs.add("⚠️ No se identificaron niveles de elevación legibles en el perfil PDF.");
        } else {
            logs.add(0, String.format("✅ Se actualizaron las alturas de %d canchas en el Dique Principal.", principalCount));
        }

        return new ImportReportResult(principalCount, 0, logs);
    }

    // 2. Procesar Reporte Canchas PDF (Actualiza Estados y Comentarios)
    public ImportReportResult processCanchasPdf(MultipartFile file, String updatedBy) throws IOException {
        return processCanchasBytes(file.getBytes(), updatedBy);
    }

    public ImportReportResult processCanchasBytes(byte[] pdfBytes, String updatedBy) throws IOException {
        List<String> logs = new ArrayList<>();
        int principalCount = 0;
        int lateralCount = 0;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);
            String[] lines = fullText.split("\\r?\\n");

            CurrentSection currentSection = CurrentSection.DIQUE_PRINCIPAL;
            Pattern rowPattern = Pattern.compile("^C[-_ ]?(\\d{1,2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Pattern capaPattern = Pattern.compile("Capa\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) continue;

                String lower = line.toLowerCase();
                if (lower.contains("dique lateral")) {
                    currentSection = CurrentSection.DIQUE_LATERAL;
                    continue;
                } else if (lower.contains("dique principal")) {
                    currentSection = CurrentSection.DIQUE_PRINCIPAL;
                    continue;
                }

                Matcher matcher = rowPattern.matcher(line);
                if (matcher.find()) {
                    int canchaNum = Integer.parseInt(matcher.group(1));
                    String rest = matcher.group(2).trim();

                    if (canchaNum <= 0 || canchaNum > 30) continue;

                    CanchaStatus status = parseStatusFromToken(rest);

                    if (currentSection == CurrentSection.DIQUE_PRINCIPAL) {
                        String comment = extractCommentFromRest(rest, status);
                        Cancha cancha = canchaRepository.findByNumber(canchaNum)
                                .orElseGet(() -> Cancha.builder().number(canchaNum).build());

                        if (status != null) cancha.setStatus(status);
                        if (comment != null && !comment.isEmpty()) cancha.setComment(comment);

                        cancha.setLastUpdatedBy(updatedBy != null ? updatedBy : "CANCHAS_REPORT_PDF");
                        canchaRepository.save(cancha);
                        principalCount++;

                        logs.add(String.format("Dique Principal C-%02d -> Estado: %s %s",
                                canchaNum,
                                status != null ? status : "CONSERVADO",
                                (comment != null && !comment.isEmpty()) ? "| Comentario: " + comment : ""
                        ).trim());

                    } else { // DIQUE LATERAL
                        CanchaCapa canchaCapa = canchaCapaRepository.findByNumber(canchaNum)
                                .orElseGet(() -> CanchaCapa.builder().number(canchaNum).build());

                        Matcher capaMatcher = capaPattern.matcher(rest);
                        if (capaMatcher.find()) {
                            canchaCapa.setCurrentCapa(Integer.parseInt(capaMatcher.group(1)));
                        }

                        if (status != null) canchaCapa.setStatus(status);
                        canchaCapa.setLastUpdatedBy(updatedBy != null ? updatedBy : "CANCHAS_REPORT_PDF");
                        canchaCapaRepository.save(canchaCapa);
                        lateralCount++;

                        logs.add(String.format("Dique Lateral C-%02d -> Estado: %s %s",
                                canchaNum,
                                status != null ? status : "CONSERVADO",
                                canchaCapa.getCurrentCapa() != null ? "| Capa: " + canchaCapa.getCurrentCapa() : ""
                        ).trim());
                    }
                }
            }
        }

        if (principalCount == 0 && lateralCount == 0) {
            logs.add("⚠️ No se identificaron filas de canchas en el reporte PDF.");
        } else {
            logs.add(0, String.format("✅ Se actualizaron estados y comentarios de %d canchas en Dique Principal y %d en Dique Lateral.", principalCount, lateralCount));
        }

        return new ImportReportResult(principalCount, lateralCount, logs);
    }

    private String extractCommentFromRest(String rest, CanchaStatus status) {
        if (rest == null || rest.trim().isEmpty()) return "";
        String text = rest.trim();

        // Quitar la palabra clave del estado del comentario
        String[] statusKeywords = {
                "Por ciclonear.", "Por ciclonear", "Cicloneando.", "Cicloneando",
                "Por compactar.", "Por compactar", "Compactado.", "Compactado", "Compactada.", "Compactada", "Finalizado.", "Finalizado",
                "Por preparar.", "Por preparar", "Drenando.", "Drenando",
                "STAND BY.", "STAND BY", "STANDBY.", "STANDBY", "OBSERVADA.", "OBSERVADA"
        };

        for (String kw : statusKeywords) {
            if (text.startsWith(kw)) {
                text = text.substring(kw.length()).trim();
                break;
            }
        }

        return text;
    }

    public CanchaStatus parseStatusFromToken(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase();

        // 1. Simbología de Perfil Topográfico Geotecnia
        if (trimmed.equalsIgnoreCase("CYp") || trimmed.equalsIgnoreCase("CYP")) return CanchaStatus.STAND_BY;
        if (trimmed.equals("/Cy") || trimmed.equalsIgnoreCase("/cy")) return CanchaStatus.POR_CICLONEAR;
        if (trimmed.equals("Cy") || trimmed.equalsIgnoreCase("cy")) return CanchaStatus.CICLONEANDO;
        if (trimmed.equals("/Cp") || trimmed.equalsIgnoreCase("/cp")) return CanchaStatus.POR_COMPACTAR;
        if (trimmed.equals("Cp") || trimmed.equalsIgnoreCase("cp")) return CanchaStatus.COMPACTADO;
        if (trimmed.equals("/Pp") || trimmed.equalsIgnoreCase("/pp")) return CanchaStatus.POR_PREPARAR_BERMA;
        if (trimmed.equals("Dr") || trimmed.equalsIgnoreCase("dr")) return CanchaStatus.DRENANDO;
        if (trimmed.equalsIgnoreCase("Sb")) return CanchaStatus.STAND_BY;
        if (trimmed.equalsIgnoreCase("Obs")) return CanchaStatus.OBSERVADA;

        // 2. Palabras completas en reportes tabulares
        if (lower.contains("por ciclonear")) return CanchaStatus.POR_CICLONEAR;
        if (lower.contains("cicloneando")) return CanchaStatus.CICLONEANDO;
        if (lower.contains("por compactar")) return CanchaStatus.POR_COMPACTAR;
        if (lower.contains("compactado") || lower.contains("compactada") || lower.contains("finalizado")) return CanchaStatus.COMPACTADO;
        if (lower.contains("por preparar")) return CanchaStatus.POR_PREPARAR_BERMA;
        if (lower.contains("drenando")) return CanchaStatus.DRENANDO;
        if (lower.contains("stand by") || lower.contains("standby")) return CanchaStatus.STAND_BY;
        if (lower.contains("observada")) return CanchaStatus.OBSERVADA;

        return null;
    }
}
