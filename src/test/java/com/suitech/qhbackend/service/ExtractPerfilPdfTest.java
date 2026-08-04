package com.suitech.qhbackend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractPerfilPdfTest {

    @Test
    public void testPerfilExtractionAlgorithm() throws Exception {
        File file = new File("D:/qhrelavera/perfil.pdf");
        if (!file.exists()) return;

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            String[] lines = text.split("\\r?\\n");

            Pattern canchaPattern = Pattern.compile("^C[-_ ]?(\\d{1,2})$", Pattern.CASE_INSENSITIVE);
            Pattern elevationPattern = Pattern.compile("\\b(1[0-2]\\d{2}\\.\\d{2})\\b");

            Map<Integer, Double> heights = new TreeMap<>();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                Matcher m = canchaPattern.matcher(line);
                if (m.find()) {
                    int num = Integer.parseInt(m.group(1));
                    if (num <= 0 || num > 30) continue;

                    // Probar offsets en orden prioritario: mismo renglón, i+1 (debajo), i-1 (arriba), i+2, i-2
                    int[] offsets = {0, 1, -1, 2, -2};
                    Double found = null;

                    for (int offset : offsets) {
                        int idx = i + offset;
                        if (idx >= 0 && idx < lines.length) {
                            Matcher elevMatcher = elevationPattern.matcher(lines[idx].trim());
                            if (elevMatcher.find()) {
                                double val = Double.parseDouble(elevMatcher.group(1));
                                if (val >= 1000.0 && val <= 1300.0 && val != 1220.0 && val != 1215.0) {
                                    found = val;
                                    break;
                                }
                            }
                        }
                    }

                    if (found != null && !heights.containsKey(num)) {
                        heights.put(num, found);
                    }
                }
            }

            System.out.println("=== EXTRACTED ELEVATION LEVELS FOR DIQUE PRINCIPAL ===");
            for (Map.Entry<Integer, Double> entry : heights.entrySet()) {
                System.out.printf("C-%02d => %.2f msnm%n", entry.getKey(), entry.getValue());
            }
            System.out.println("Total extracted: " + heights.size());
        }
    }
}
