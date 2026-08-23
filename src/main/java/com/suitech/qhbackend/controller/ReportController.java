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
