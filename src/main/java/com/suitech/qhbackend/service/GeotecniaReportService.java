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
        NONE,
        DIQUE_PRINCIPAL,
        DIQUE_LATERAL
    }

    public ImportReportResult processPdfReport(MultipartFile file, String updatedBy) throws IOException {
        List<String> logs = new ArrayList<>();
        int principalCount = 0;
        int lateralCount = 0;

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            String[] lines = fullText.split("\\r?\\n");
            CurrentSection section = CurrentSection.NONE;

            Pattern canchaPattern = Pattern.compile("^C[-_ ]?(\\d{1,2})\\b(.*)", Pattern.CASE_INSENSITIVE);
            Pattern capaPattern = Pattern.compile("Capa\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) continue;

                String lower = line.toLowerCase();

                if (lower.contains("dique principal") || (lower.contains("nivel 1220") && !lower.contains("dique lateral"))) {
                    section = CurrentSection.DIQUE_PRINCIPAL;
                    logs.add("Iniciando sección: Dique Principal");
                    continue;
                } else if (lower.contains("dique lateral") || lower.contains("nivel 1215")) {
                    section = CurrentSection.DIQUE_LATERAL;
                    logs.add("Iniciando sección: Dique Lateral");
                    continue;
                }

                Matcher m = canchaPattern.matcher(line);
                if (m.find()) {
                    int canchaNumber = Integer.parseInt(m.group(1));
                    String restOfLine = m.group(2).trim();

                    if (section == CurrentSection.DIQUE_PRINCIPAL) {
                        CanchaStatus status = determineStatus(restOfLine);
                        if (status != null) {
                            Cancha cancha = canchaRepository.findByNumber(canchaNumber)
                                    .orElseGet(() -> Cancha.builder().number(canchaNumber).build());

                            cancha.setStatus(status);
                            String comment = extractComment(restOfLine, status);
                            if (!comment.isEmpty()) {
                                cancha.setComment(comment);
                            }
                            cancha.setLastUpdatedBy(updatedBy != null ? updatedBy : "SISTEMA_PDF");
                            canchaRepository.save(cancha);
                            principalCount++;
                            logs.add("Dique Principal C-" + String.format("%02d", canchaNumber) + " -> Estado: " + status);
                        }
                    } else if (section == CurrentSection.DIQUE_LATERAL) {
                        CanchaStatus status = determineStatus(restOfLine);
                        if (status != null) {
                            CanchaCapa canchaCapa = canchaCapaRepository.findByNumber(canchaNumber)
                                    .orElseGet(() -> CanchaCapa.builder().number(canchaNumber).build());

                            canchaCapa.setStatus(status);

                            // Extraer capa si está presente
                            Matcher capaMatcher = capaPattern.matcher(restOfLine);
                            if (capaMatcher.find()) {
                                int capaNum = Integer.parseInt(capaMatcher.group(1));
                                canchaCapa.setCurrentCapa(capaNum);
                            }

                            String comment = extractComment(restOfLine, status);
                            if (!comment.isEmpty()) {
                                canchaCapa.setComment(comment);
                            }
                            canchaCapa.setLastUpdatedBy(updatedBy != null ? updatedBy : "SISTEMA_PDF");
                            canchaCapaRepository.save(canchaCapa);
                            lateralCount++;
                            logs.add("Dique Lateral C-" + String.format("%02d", canchaNumber) + " -> Estado: " + status);
                        }
                    }
                }
            }
        }

        return new ImportReportResult(principalCount, lateralCount, logs);
    }

    private CanchaStatus determineStatus(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("cicloneando")) return CanchaStatus.CICLONEANDO;
        if (lower.contains("por ciclonear")) return CanchaStatus.POR_CICLONEAR;
        if (lower.contains("por compactar")) return CanchaStatus.POR_COMPACTAR;
        if (lower.contains("compactado") || lower.contains("compactada")) return CanchaStatus.COMPACTADO;
        if (lower.contains("por preparar")) return CanchaStatus.POR_PREPARAR_BERMA;
        if (lower.contains("drenando")) return CanchaStatus.DRENANDO;
        if (lower.contains("stand by") || lower.contains("standby")) return CanchaStatus.STAND_BY;
        if (lower.contains("observada")) return CanchaStatus.OBSERVADA;
        if (lower.contains("finalizado")) return CanchaStatus.COMPACTADO;
        return null;
    }

    private String extractComment(String text, CanchaStatus status) {
        String clean = text
                .replaceAll("(?i)por ciclonear", "")
                .replaceAll("(?i)cicloneando", "")
                .replaceAll("(?i)por compactar", "")
                .replaceAll("(?i)compactado", "")
                .replaceAll("(?i)compactada", "")
                .replaceAll("(?i)por preparar", "")
                .replaceAll("(?i)drenando", "")
                .replaceAll("(?i)stand by", "")
                .replaceAll("(?i)standby", "")
                .replaceAll("(?i)finalizado", "")
                .replaceAll("(?i)capa\\s*\\d+", "")
                .replaceAll("(?i)estado del talud", "")
                .replaceAll("(?i)n° capa del talud", "")
                .trim();

        clean = clean.replaceAll("^[\\.\\,\\-\\s]+", "").trim();
        return clean;
    }
}
