package com.suitech.qhbackend.service;

import com.suitech.qhbackend.model.DailyReport;
import com.suitech.qhbackend.repository.DailyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PastedReportService {

    private final DailyReportRepository dailyReportRepository;

    @Transactional
    public Map<String, Object> parseAndSavePastedReport(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("El texto pegado está vacío.");
        }

        String[] lines = rawText.split("\r?\n");
        int count = 0;
        Set<String> yearMonthKeys = new HashSet<>();
        Integer primaryYear = null;
        Integer primaryMonth = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || isHeaderLine(trimmed)) {
                continue;
            }

            String[] tokens = splitLineTokens(trimmed);
            if (tokens.length < 4) {
                continue;
            }

            String dateStr = tokens[0].trim();
            String turnoStr = tokens[1].trim().toUpperCase();
            String dpStr = tokens[2].trim();
            String dlStr = tokens[3].trim();

            if (!turnoStr.equals("A") && !turnoStr.equals("B")) {
                continue;
            }

            LocalDate reportDate = parsePastedDate(dateStr);
            if (reportDate == null) {
                continue;
            }

            double dp = parsePastedNumber(dpStr);
            double dl = parsePastedNumber(dlStr);

            int year = reportDate.getYear();
            int month = reportDate.getMonthValue();
            int day = reportDate.getDayOfMonth();

            primaryYear = year;
            primaryMonth = month;
            yearMonthKeys.add(year + "-" + month);

            Optional<DailyReport> existingOpt = dailyReportRepository.findByReportDate(reportDate);
            DailyReport report = existingOpt.orElseGet(() -> DailyReport.builder()
                    .reportDate(reportDate)
                    .yearNumber(year)
                    .monthNumber(month)
                    .dayNumber(day)
                    .dpArenasGuardiaA(0.0)
                    .dpArenasGuardiaB(0.0)
                    .dpArenasTotalDia(0.0)
                    .dlArenasGuardiaA(0.0)
                    .dlArenasGuardiaB(0.0)
                    .dlArenasTotalDia(0.0)
                    .totalArenasGuardiaA(0.0)
                    .totalArenasGuardiaB(0.0)
                    .totalArenasDia(0.0)
                    .build());

            if (turnoStr.equals("A")) {
                report.setDpArenasGuardiaA(dp);
                report.setDlArenasGuardiaA(dl);
            } else if (turnoStr.equals("B")) {
                report.setDpArenasGuardiaB(dp);
                report.setDlArenasGuardiaB(dl);
            }

            double dpA = report.getDpArenasGuardiaA() != null ? report.getDpArenasGuardiaA() : 0.0;
            double dpB = report.getDpArenasGuardiaB() != null ? report.getDpArenasGuardiaB() : 0.0;
            double dlA = report.getDlArenasGuardiaA() != null ? report.getDlArenasGuardiaA() : 0.0;
            double dlB = report.getDlArenasGuardiaB() != null ? report.getDlArenasGuardiaB() : 0.0;

            report.setDpArenasTotalDia(dpA + dpB);
            report.setDlArenasTotalDia(dlA + dlB);
            report.setTotalArenasGuardiaA(dpA + dlA);
            report.setTotalArenasGuardiaB(dpB + dlB);
            report.setTotalArenasDia(dpA + dpB + dlA + dlB);

            dailyReportRepository.save(report);
            count++;
        }

        if (count == 0) {
            throw new IllegalArgumentException("No se pudieron interpretar filas válidas. Verifica el formato: DIA | TURNO | DP | DL");
        }

        // Garantizar que todos los días de cada mes procesado existan en la BD
        for (String ymKey : yearMonthKeys) {
            String[] parts = ymKey.split("-");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int maxDays = YearMonth.of(y, m).lengthOfMonth();

            for (int d = 1; d <= maxDays; d++) {
                LocalDate rDate = LocalDate.of(y, m, d);
                if (!dailyReportRepository.findByReportDate(rDate).isPresent()) {
                    DailyReport emptyReport = DailyReport.builder()
                            .reportDate(rDate)
                            .yearNumber(y)
                            .monthNumber(m)
                            .dayNumber(d)
                            .dpArenasGuardiaA(0.0)
                            .dpArenasGuardiaB(0.0)
                            .dpArenasTotalDia(0.0)
                            .dlArenasGuardiaA(0.0)
                            .dlArenasGuardiaB(0.0)
                            .dlArenasTotalDia(0.0)
                            .totalArenasGuardiaA(0.0)
                            .totalArenasGuardiaB(0.0)
                            .totalArenasDia(0.0)
                            .build();
                    dailyReportRepository.save(emptyReport);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("recordsProcessed", count);
        result.put("year", primaryYear);
        result.put("month", primaryMonth);
        result.put("message", "Producción pegada (" + count + " registros) guardada exitosamente.");
        return result;
    }

    private boolean isHeaderLine(String line) {
        String upper = line.toUpperCase();
        return upper.contains("DIA") || upper.contains("FECHA") || upper.contains("TURNO") || upper.contains("DP") || upper.contains("DL");
    }

    private String[] splitLineTokens(String line) {
        if (line.contains("\t")) {
            return line.split("\t");
        }
        if (line.contains(";")) {
            return line.split(";");
        }
        return line.split("\\s{2,}");
    }

    private LocalDate parsePastedDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String cleaned = raw.trim();

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
                if (p1 > 1000) {
                    year = p1;
                    month = p2;
                    day = p3;
                } else {
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

    private double parsePastedNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0.0;
        try {
            String cleaned = raw.trim().replace(" ", "");
            if (cleaned.contains(".") && cleaned.contains(",")) {
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else if (cleaned.contains(",")) {
                cleaned = cleaned.replace(",", "");
            }
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
