package com.suitech.qhbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PastedReportParserTest {

    @Test
    public void testPastedTextParsing() {
        String sampleText = "DIA\tTURNO\tDP\tDL\n" +
                "1-Jan-26\tA\t20,962\t3,550\n" +
                "1-Jan-26\tB\t22,198\t2,989\n" +
                "2-Jan-26\tA\t20,961\t1,495\n" +
                "2-Jan-26\tB\t21,091\t2,989\n" +
                "23-Jan-26\tA\t19,866\t3,683\n" +
                "31-Jan-26\tB\t22,191\t5,060";

        String[] lines = sampleText.split("\r?\n");
        int count = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.toUpperCase().contains("DIA") || line.toUpperCase().contains("TURNO")) {
                continue;
            }

            String[] tokens = splitLineTokens(line);
            assertTrue(tokens.length >= 4, "Debe tener al menos 4 columnas: " + line);

            String dateStr = tokens[0].trim();
            String turnoStr = tokens[1].trim();
            String dpStr = tokens[2].trim();
            String dlStr = tokens[3].trim();

            LocalDate date = parsePastedDate(dateStr);
            assertNotNull(date, "Fecha no debe ser nula para: " + dateStr);

            double dp = parsePastedNumber(dpStr);
            double dl = parsePastedNumber(dlStr);

            System.out.printf("Registro %d: Fecha=%s, Turno=%s, DP=%.1f, DL=%.1f\n",
                    ++count, date, turnoStr, dp, dl);

            if (count == 1) {
                assertEquals(20962.0, dp);
                assertEquals(3550.0, dl);
            }
        }

        assertEquals(6, count);
    }

    public static String[] splitLineTokens(String line) {
        if (line.contains("\t")) {
            return line.split("\t");
        }
        if (line.contains(";")) {
            return line.split(";");
        }
        return line.split("\\s{2,}");
    }

    public static LocalDate parsePastedDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String cleaned = raw.trim();

        // Reemplazar meses en palabras (Ingles y Español)
        cleaned = cleaned.replaceAll("(?i)Jan|Ene", "01")
                .replaceAll("(?i)Feb", "02")
                .replaceAll("(?i)Mar", "03")
                .replaceAll("(?i)Apr|Abr", "04")
                .replaceAll("(?i)May", "05")
                .replaceAll("(?i)Jun", "06")
                .replaceAll("(?i)Jul", "07")
                .replaceAll("(?i)Aug|Ago", "08")
                .replaceAll("(?i)Sep|Set", "09")
                .replaceAll("(?i)Oct", "10")
                .replaceAll("(?i)Nov", "11")
                .replaceAll("(?i)Dec|Dic", "12");

        String[] parts = cleaned.split("[-/._\\s]+");
        if (parts.length >= 3) {
            try {
                int p1 = Integer.parseInt(parts[0]);
                int p2 = Integer.parseInt(parts[1]);
                int p3 = Integer.parseInt(parts[2]);

                int year, month, day;
                if (p1 > 1000) { // Formato YYYY-MM-DD
                    year = p1;
                    month = p2;
                    day = p3;
                } else { // Formato DD-MM-YYYY o DD-MM-YY
                    day = p1;
                    month = p2;
                    year = p3 < 100 ? 2000 + p3 : p3;
                }
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static double parsePastedNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0.0;
        try {
            String cleaned = raw.trim().replace(" ", "");
            // Si contiene coma y punto (ej 20.962,50)
            if (cleaned.contains(".") && cleaned.contains(",")) {
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else if (cleaned.contains(",")) {
                // Si la coma es separador de miles ej "20,962"
                cleaned = cleaned.replace(",", "");
            }
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
