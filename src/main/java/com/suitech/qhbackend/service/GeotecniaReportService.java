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

    public ImportReportResult processPdfReport(MultipartFile file, String updatedBy) throws IOException {
        return processPdfBytes(file.getBytes(), updatedBy);
    }

    public ImportReportResult processPdfBytes(byte[] pdfBytes, String updatedBy) throws IOException {
        List<String> logs = new ArrayList<>();
        int principalCount = 0;
        int lateralCount = 0;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            String[] lines = fullText.split("\\r?\\n");

            // Mapa para registrar actualizaciones por número de cancha
            Map<Integer, Double> principalHeights = new HashMap<>();
            Map<Integer, CanchaStatus> principalStatuses = new HashMap<>();

            Map<Integer, Double> lateralHeights = new HashMap<>();
            Map<Integer, CanchaStatus> lateralStatuses = new HashMap<>();
            Map<Integer, Integer> lateralCapas = new HashMap<>();

            CurrentSection currentSection = CurrentSection.DIQUE_PRINCIPAL;

            Pattern canchaTokenPattern = Pattern.compile("\\bC[-_ ]?(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE);
            Pattern elevationPattern = Pattern.compile("\\b(1[0-2]\\d{2}\\.\\d{1,2}|\\d{4}\\.\\d{1,2})\\b");
            Pattern capaPattern = Pattern.compile("Capa\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                String lower = line.toLowerCase();
                if (lower.contains("dique lateral") || lower.contains("nivel 1215")) {
                    currentSection = CurrentSection.DIQUE_LATERAL;
                } else if (lower.contains("dique principal") || lower.contains("nivel 1220")) {
                    currentSection = CurrentSection.DIQUE_PRINCIPAL;
                }

                Matcher canchaMatcher = canchaTokenPattern.matcher(line);
                while (canchaMatcher.find()) {
                    int canchaNum = Integer.parseInt(canchaMatcher.group(1));
                    if (canchaNum <= 0 || canchaNum > 30) continue;

                    // Buscar elevación/altura y estado en las líneas cercanas (i-4 a i+4)
                    Double foundHeight = null;
                    CanchaStatus foundStatus = null;
                    Integer foundCapa = null;

                    int windowStart = Math.max(0, i - 4);
                    int windowEnd = Math.min(lines.length - 1, i + 4);

                    for (int j = windowStart; j <= windowEnd; j++) {
                        String contextLine = lines[j].trim();

                        // Buscar elevaciones (ej. 1123.32, 1156.27, 1220.00)
                        if (foundHeight == null) {
                            Matcher elevMatcher = elevationPattern.matcher(contextLine);
                            if (elevMatcher.find()) {
                                double val = Double.parseDouble(elevMatcher.group(1));
                                // Filtrar valores de progresivas o coronamiento si no parecen de la cancha
                                if (val >= 1000.0 && val <= 1300.0) {
                                    foundHeight = val;
                                }
                            }
                        }

                        // Buscar Estado
                        if (foundStatus == null) {
                            foundStatus = parseStatusFromToken(contextLine);
                        }

                        // Buscar N° de Capa
                        if (foundCapa == null) {
                            Matcher capaMatcher = capaPattern.matcher(contextLine);
                            if (capaMatcher.find()) {
                                foundCapa = Integer.parseInt(capaMatcher.group(1));
                            }
                        }
                    }

                    if (currentSection == CurrentSection.DIQUE_PRINCIPAL) {
                        if (foundHeight != null && !principalHeights.containsKey(canchaNum)) {
                            principalHeights.put(canchaNum, foundHeight);
                        }
                        if (foundStatus != null && !principalStatuses.containsKey(canchaNum)) {
                            principalStatuses.put(canchaNum, foundStatus);
                        }
                    } else {
                        if (foundHeight != null && !lateralHeights.containsKey(canchaNum)) {
                            lateralHeights.put(canchaNum, foundHeight);
                        }
                        if (foundStatus != null && !lateralStatuses.containsKey(canchaNum)) {
                            lateralStatuses.put(canchaNum, foundStatus);
                        }
                        if (foundCapa != null && !lateralCapas.containsKey(canchaNum)) {
                            lateralCapas.put(canchaNum, foundCapa);
                        }
                    }
                }
            }

            // Aplicar actualizaciones a Dique Principal
            Set<Integer> allPrincipalCanchas = new TreeSet<>(principalHeights.keySet());
            allPrincipalCanchas.addAll(principalStatuses.keySet());

            for (Integer num : allPrincipalCanchas) {
                Cancha cancha = canchaRepository.findByNumber(num)
                        .orElseGet(() -> Cancha.builder().number(num).build());

                Double h = principalHeights.get(num);
                CanchaStatus st = principalStatuses.get(num);

                if (h != null) cancha.setCurrentHeight(h);
                if (st != null) cancha.setStatus(st);

                cancha.setLastUpdatedBy(updatedBy != null ? updatedBy : "SISTEMA_GEOTECNIA_PDF");
                canchaRepository.save(cancha);
                principalCount++;

                String logMsg = String.format("Dique Principal C-%02d -> %s %s",
                        num,
                        h != null ? "Altura: " + h + " m" : "",
                        st != null ? "| Estado: " + st : ""
                ).trim();
                logs.add(logMsg);
            }

            // Aplicar actualizaciones a Dique Lateral
            Set<Integer> allLateralCanchas = new TreeSet<>(lateralHeights.keySet());
            allLateralCanchas.addAll(lateralStatuses.keySet());

            for (Integer num : allLateralCanchas) {
                CanchaCapa canchaCapa = canchaCapaRepository.findByNumber(num)
                        .orElseGet(() -> CanchaCapa.builder().number(num).build());

                Double h = lateralHeights.get(num);
                CanchaStatus st = lateralStatuses.get(num);
                Integer capa = lateralCapas.get(num);

                if (st != null) canchaCapa.setStatus(st);
                if (capa != null) canchaCapa.setCurrentCapa(capa);

                canchaCapa.setLastUpdatedBy(updatedBy != null ? updatedBy : "SISTEMA_GEOTECNIA_PDF");
                canchaCapaRepository.save(canchaCapa);
                lateralCount++;

                String logMsg = String.format("Dique Lateral C-%02d -> %s %s",
                        num,
                        capa != null ? "Capa: " + capa : "",
                        st != null ? "| Estado: " + st : ""
                ).trim();
                logs.add(logMsg);
            }
        }

        if (principalCount == 0 && lateralCount == 0) {
            logs.add("⚠️ No se identificaron niveles o estados de canchas legibles en el PDF subido.");
        } else {
            logs.add(0, String.format("✅ Se actualizaron exitosamente %d canchas en Dique Principal y %d en Dique Lateral.", principalCount, lateralCount));
        }

        return new ImportReportResult(principalCount, lateralCount, logs);
    }

    private CanchaStatus parseStatusFromToken(String text) {
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
        if (lower.contains("compactado") || lower.contains("compactada")) return CanchaStatus.COMPACTADO;
        if (lower.contains("por preparar")) return CanchaStatus.POR_PREPARAR_BERMA;
        if (lower.contains("drenando")) return CanchaStatus.DRENANDO;
        if (lower.contains("stand by") || lower.contains("standby")) return CanchaStatus.STAND_BY;
        if (lower.contains("observada")) return CanchaStatus.OBSERVADA;

        return null;
    }
}
