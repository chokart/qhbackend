package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.model.DailyReport;
import com.suitech.qhbackend.model.SapNotice;
import com.suitech.qhbackend.repository.DailyReportRepository;
import com.suitech.qhbackend.repository.SapNoticeRepository;
import com.suitech.qhbackend.service.ExcelReportParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final ExcelReportParserService parserService;
    private final DailyReportRepository dailyReportRepository;
    private final SapNoticeRepository sapNoticeRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcelReport(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El archivo subido está vacío."));
        }
        try {
            Map<String, Object> result = parserService.parseAndSaveExcelReport(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Error al procesar el archivo Excel: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/months")
    public ResponseEntity<?> getAvailableMonths() {
        List<Object[]> rows = dailyReportRepository.findAvailableMonths();
        List<Map<String, Integer>> list = new ArrayList<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                Map<String, Integer> m = new HashMap<>();
                m.put("year", ((Number) row[0]).intValue());
                m.put("month", ((Number) row[1]).intValue());
                list.add(m);
            }
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month
    ) {
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        List<DailyReport> dailyReports = dailyReportRepository
                .findByYearNumberAndMonthNumberOrderByDayNumberAsc(year, month);

        // Garantizar que existan registros para TODOS los días del mes (01 al 28/29/30/31)
        int maxDaysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
        Set<Integer> existingDays = dailyReports.stream()
                .map(DailyReport::getDayNumber)
                .collect(java.util.stream.Collectors.toSet());

        boolean addedMissing = false;
        for (int day = 1; day <= maxDaysInMonth; day++) {
            if (!existingDays.contains(day)) {
                LocalDate reportDate = LocalDate.of(year, month, day);
                DailyReport emptyReport = DailyReport.builder()
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
                        .build();
                dailyReportRepository.save(emptyReport);
                addedMissing = true;
            }
        }

        if (addedMissing) {
            dailyReports = dailyReportRepository
                    .findByYearNumberAndMonthNumberOrderByDayNumberAsc(year, month);
        }

        List<SapNotice> sapNotices = sapNoticeRepository
                .findByReportYearAndReportMonth(year, month);

        // Resumen Acumulado por Dique y Turno
        double totalDpArenasA = dailyReports.stream()
                .mapToDouble(d -> d.getDpArenasGuardiaA() != null ? d.getDpArenasGuardiaA() : 0.0).sum();

        double totalDpArenasB = dailyReports.stream()
                .mapToDouble(d -> d.getDpArenasGuardiaB() != null ? d.getDpArenasGuardiaB() : 0.0).sum();

        double totalDpArenas = dailyReports.stream()
                .mapToDouble(d -> d.getDpArenasTotalDia() != null ? d.getDpArenasTotalDia() : 0.0).sum();

        double totalDlArenasA = dailyReports.stream()
                .mapToDouble(d -> d.getDlArenasGuardiaA() != null ? d.getDlArenasGuardiaA() : 0.0).sum();

        double totalDlArenasB = dailyReports.stream()
                .mapToDouble(d -> d.getDlArenasGuardiaB() != null ? d.getDlArenasGuardiaB() : 0.0).sum();

        double totalDlArenas = dailyReports.stream()
                .mapToDouble(d -> d.getDlArenasTotalDia() != null ? d.getDlArenasTotalDia() : 0.0).sum();

        double totalArenasA = dailyReports.stream()
                .mapToDouble(d -> d.getTotalArenasGuardiaA() != null ? d.getTotalArenasGuardiaA() : (
                        (d.getDpArenasGuardiaA() != null ? d.getDpArenasGuardiaA() : 0.0) +
                        (d.getDlArenasGuardiaA() != null ? d.getDlArenasGuardiaA() : 0.0)
                )).sum();

        double totalArenasB = dailyReports.stream()
                .mapToDouble(d -> d.getTotalArenasGuardiaB() != null ? d.getTotalArenasGuardiaB() : (
                        (d.getDpArenasGuardiaB() != null ? d.getDpArenasGuardiaB() : 0.0) +
                        (d.getDlArenasGuardiaB() != null ? d.getDlArenasGuardiaB() : 0.0)
                )).sum();

        double totalArenasMes = dailyReports.stream()
                .mapToDouble(d -> d.getTotalArenasDia() != null ? d.getTotalArenasDia() : 0.0).sum();

        double avgNivelPresaDp = dailyReports.stream()
                .mapToDouble(d -> d.getNivelPresaDpMsnm() != null ? d.getNivelPresaDpMsnm() : 0.0)
                .filter(val -> val > 0).average().orElse(0.0);

        double avgNivelPresaDl = dailyReports.stream()
                .mapToDouble(d -> d.getNivelPresaDlMsnm() != null ? d.getNivelPresaDlMsnm() : 0.0)
                .filter(val -> val > 0).average().orElse(0.0);

        double avgNivelAgua = dailyReports.stream()
                .mapToDouble(d -> d.getNivelAguaMsnm() != null ? d.getNivelAguaMsnm() : 0.0)
                .filter(val -> val > 0).average().orElse(0.0);

        long activeSapCount = sapNotices.stream()
                .filter(s -> s.getStatus() != null && s.getStatus().equalsIgnoreCase("Reportado")).count();

        Map<String, Object> response = new HashMap<>();
        response.put("year", year);
        response.put("month", month);
        response.put("daysCount", dailyReports.size());
        response.put("totalDpArenasA", totalDpArenasA);
        response.put("totalDpArenasB", totalDpArenasB);
        response.put("totalDpArenas", totalDpArenas);
        response.put("totalDlArenasA", totalDlArenasA);
        response.put("totalDlArenasB", totalDlArenasB);
        response.put("totalDlArenas", totalDlArenas);
        response.put("totalArenasA", totalArenasA);
        response.put("totalArenasB", totalArenasB);
        response.put("totalArenasMes", totalArenasMes);
        response.put("avgNivelPresaDp", avgNivelPresaDp);
        response.put("avgNivelPresaDl", avgNivelPresaDl);
        response.put("avgNivelAgua", avgNivelAgua);
        response.put("activeSapCount", activeSapCount);
        response.put("dailyReports", dailyReports);
        response.put("sapNotices", sapNotices);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/daily")
    public ResponseEntity<?> getDailyReport(@RequestParam("date") String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            Optional<DailyReport> opt = dailyReportRepository.findByReportDate(date);
            if (opt.isPresent()) {
                return ResponseEntity.ok(opt.get());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Fecha inválida"));
        }
    }

    @PutMapping("/daily/{id}")
    public ResponseEntity<?> updateDailyReport(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> body
    ) {
        Optional<DailyReport> opt = dailyReportRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DailyReport report = opt.get();

        double dpA = parseDouble(body.get("dpArenasGuardiaA"));
        double dpB = parseDouble(body.get("dpArenasGuardiaB"));
        double dlA = parseDouble(body.get("dlArenasGuardiaA"));
        double dlB = parseDouble(body.get("dlArenasGuardiaB"));

        report.setDpArenasGuardiaA(dpA);
        report.setDpArenasGuardiaB(dpB);
        report.setDpArenasTotalDia(dpA + dpB);

        report.setDlArenasGuardiaA(dlA);
        report.setDlArenasGuardiaB(dlB);
        report.setDlArenasTotalDia(dlA + dlB);

        report.setTotalArenasGuardiaA(dpA + dlA);
        report.setTotalArenasGuardiaB(dpB + dlB);
        report.setTotalArenasDia(dpA + dpB + dlA + dlB);

        dailyReportRepository.save(report);
        return ResponseEntity.ok(report);
    }

    private double parseDouble(Object val) {
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    @GetMapping("/annual")
    public ResponseEntity<?> getAnnualSummary(@RequestParam(name = "year", required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        List<DailyReport> yearReports = dailyReportRepository.findByYearNumberOrderByMonthNumberAscDayNumberAsc(year);

        Map<Integer, List<DailyReport>> byMonth = yearReports.stream()
                .collect(Collectors.groupingBy(DailyReport::getMonthNumber));

        List<Map<String, Object>> monthsSummary = new ArrayList<>();

        double grandDpA = 0, grandDpB = 0, grandDpTot = 0;
        double grandDlA = 0, grandDlB = 0, grandDlTot = 0;
        double grandTotA = 0, grandTotB = 0, grandTotYear = 0;

        for (int m = 1; m <= 12; m++) {
            List<DailyReport> mReports = byMonth.getOrDefault(m, Collections.emptyList());

            double dpA = mReports.stream().mapToDouble(d -> d.getDpArenasGuardiaA() != null ? d.getDpArenasGuardiaA() : 0.0).sum();
            double dpB = mReports.stream().mapToDouble(d -> d.getDpArenasGuardiaB() != null ? d.getDpArenasGuardiaB() : 0.0).sum();
            double dpTot = mReports.stream().mapToDouble(d -> d.getDpArenasTotalDia() != null ? d.getDpArenasTotalDia() : 0.0).sum();

            double dlA = mReports.stream().mapToDouble(d -> d.getDlArenasGuardiaA() != null ? d.getDlArenasGuardiaA() : 0.0).sum();
            double dlB = mReports.stream().mapToDouble(d -> d.getDlArenasGuardiaB() != null ? d.getDlArenasGuardiaB() : 0.0).sum();
            double dlTot = mReports.stream().mapToDouble(d -> d.getDlArenasTotalDia() != null ? d.getDlArenasTotalDia() : 0.0).sum();

            double totA = mReports.stream().mapToDouble(d -> d.getTotalArenasGuardiaA() != null ? d.getTotalArenasGuardiaA() : (dpA + dlA)).sum();
            double totB = mReports.stream().mapToDouble(d -> d.getTotalArenasGuardiaB() != null ? d.getTotalArenasGuardiaB() : (dpB + dlB)).sum();
            double totMes = mReports.stream().mapToDouble(d -> d.getTotalArenasDia() != null ? d.getTotalArenasDia() : 0.0).sum();

            grandDpA += dpA;
            grandDpB += dpB;
            grandDpTot += dpTot;
            grandDlA += dlA;
            grandDlB += dlB;
            grandDlTot += dlTot;
            grandTotA += totA;
            grandTotB += totB;
            grandTotYear += totMes;

            Map<String, Object> mData = new HashMap<>();
            mData.put("monthNumber", m);
            mData.put("hasData", !mReports.isEmpty());
            mData.put("daysCount", mReports.size());
            mData.put("dpArenasA", dpA);
            mData.put("dpArenasB", dpB);
            mData.put("dpArenasTotal", dpTot);
            mData.put("dlArenasA", dlA);
            mData.put("dlArenasB", dlB);
            mData.put("dlArenasTotal", dlTot);
            mData.put("totalArenasA", totA);
            mData.put("totalArenasB", totB);
            mData.put("totalArenasMes", totMes);

            monthsSummary.add(mData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("year", year);
        response.put("months", monthsSummary);
        response.put("grandDpA", grandDpA);
        response.put("grandDpB", grandDpB);
        response.put("grandDpTotal", grandDpTot);
        response.put("grandDlA", grandDlA);
        response.put("grandDlB", grandDlB);
        response.put("grandDlTotal", grandDlTot);
        response.put("grandTotalA", grandTotA);
        response.put("grandTotalB", grandTotB);
        response.put("grandTotalYear", grandTotYear);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteReportByMonth(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month
    ) {
        try {
            dailyReportRepository.deleteByYearNumberAndMonthNumber(year, month);
            sapNoticeRepository.deleteByReportYearAndReportMonth(year, month);
            return ResponseEntity.ok(Map.of("message", "Reporte de " + month + "/" + year + " eliminado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error al eliminar el reporte: " + e.getMessage()));
        }
    }
}
