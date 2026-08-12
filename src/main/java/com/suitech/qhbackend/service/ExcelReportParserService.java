package com.suitech.qhbackend.service;

import com.suitech.qhbackend.model.DailyReport;
import com.suitech.qhbackend.model.SapNotice;
import com.suitech.qhbackend.repository.DailyReportRepository;
import com.suitech.qhbackend.repository.SapNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelReportParserService {

    private final DailyReportRepository dailyReportRepository;
    private final SapNoticeRepository sapNoticeRepository;

    @Transactional
    public Map<String, Object> parseAndSaveExcelReport(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            DataFormatter formatter = new DataFormatter();

            // 1. Detectar el Año y Mes global del libro Excel
            int[] yearMonth = detectWorkbookYearMonth(workbook, formatter);
            int year = yearMonth[0];
            int month = yearMonth[1];

            int parsedDaysCount = 0;
            int maxDaysInMonth = YearMonth.of(year, month).lengthOfMonth();

            // 2. Recorrer hojas que sean partes diarios ("01" a "31" o "1" a "31")
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName().trim();

                if (sheetName.matches("^\\d{1,2}$")) {
                    int day = Integer.parseInt(sheetName);
                    if (day >= 1 && day <= maxDaysInMonth) {
                        LocalDate reportDate = LocalDate.of(year, month, day);
                        DailyReport report = parseDailySheet(sheet, formatter, reportDate);

                        if (report != null) {
                            dailyReportRepository.findByReportDate(reportDate)
                                    .ifPresent(existing -> report.setId(existing.getId()));

                            dailyReportRepository.save(report);
                            parsedDaysCount++;
                        }
                    }
                }
            }

            // 3. Recorrer hoja "Registro aviso SAP" si existe
            int sapCount = 0;
            Sheet sapSheet = workbook.getSheet("Registro aviso SAP");
            if (sapSheet != null) {
                sapNoticeRepository.deleteByReportYearAndReportMonth(year, month);
                List<SapNotice> notices = parseSapNotices(sapSheet, formatter, year, month);
                sapNoticeRepository.saveAll(notices);
                sapCount = notices.size();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("daysProcessed", parsedDaysCount);
            result.put("year", year);
            result.put("month", month);
            result.put("sapNoticesProcessed", sapCount);
            result.put("message", "Reporte Excel procesado exitosamente.");
            return result;
        }
    }

    private int[] detectWorkbookYearMonth(Workbook workbook, DataFormatter formatter) {
        // Intentar leer fecha en celda G3 de hojas "01", "02", "1", etc.
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName().trim().matches("^\\d{1,2}$")) {
                Row r3 = sheet.getRow(2);
                Cell g3 = r3 != null ? r3.getCell(6) : null;
                LocalDate d = parseCellDate(g3);
                if (d == null) {
                    String str = getCellString(sheet, "G3", formatter);
                    d = tryParseDateString(str);
                }
                if (d != null) {
                    return new int[]{d.getYear(), d.getMonthValue()};
                }
            }
        }

        // Intentar leer en hojas de resumen como "Apex-Vortex" o "KPI"
        int year = 0;
        int month = 0;

        Sheet apexSheet = workbook.getSheet("Apex-Vortex");
        if (apexSheet != null) {
            String monthStr = getCellString(apexSheet, "K2", formatter);
            String yearStr = getCellString(apexSheet, "K3", formatter);
            month = parseMonthName(monthStr);
            try { year = Integer.parseInt(yearStr); } catch (Exception ignored) {}
        }

        if (month == 0) {
            Sheet kpiSheet = workbook.getSheet("KPI");
            if (kpiSheet != null) {
                String monthStr = getCellString(kpiSheet, "K2", formatter);
                month = parseMonthName(monthStr);
            }
        }

        if (year == 0) year = LocalDate.now().getYear();
        if (month == 0) month = LocalDate.now().getMonthValue();

        return new int[]{year, month};
    }

    private DailyReport parseDailySheet(Sheet sheet, DataFormatter formatter, LocalDate reportDate) {
        try {
            DailyReport report = DailyReport.builder()
                    .reportDate(reportDate)
                    .yearNumber(reportDate.getYear())
                    .monthNumber(reportDate.getMonthValue())
                    .dayNumber(reportDate.getDayOfMonth())
                    .build();

            // DP Arenas (Fila 6 Excel -> index 5)
            report.setDpArenasGuardiaA(getCellDouble(sheet, 5, 4));
            report.setDpArenasGuardiaB(getCellDouble(sheet, 5, 5));
            report.setDpArenasTotalDia(getCellDouble(sheet, 5, 6));
            report.setDpArenasPlanDia(getCellDouble(sheet, 5, 7));
            report.setDpArenasRealAcumMes(getCellDouble(sheet, 5, 8));
            report.setDpArenasPlanMes(getCellDouble(sheet, 5, 10));

            // Niveles Presa (Filas 7, 8, 9, 10 -> index 6, 7, 8, 9)
            report.setNivelPresaDpMsnm(getCellDouble(sheet, 6, 4));
            report.setNivelPresaDlMsnm(getCellDouble(sheet, 7, 4));
            report.setNivelAguaMsnm(getCellDouble(sheet, 8, 4));
            report.setNivelLamaM(getCellDouble(sheet, 9, 4));

            // DL Arenas (Fila 25 Excel -> index 24)
            report.setDlArenasGuardiaA(getCellDouble(sheet, 24, 4));
            report.setDlArenasGuardiaB(getCellDouble(sheet, 24, 5));
            report.setDlArenasTotalDia(getCellDouble(sheet, 24, 6));
            report.setDlArenasPlanDia(getCellDouble(sheet, 24, 7));
            report.setDlArenasRealAcumMes(getCellDouble(sheet, 24, 8));
            report.setDlArenasPlanMes(getCellDouble(sheet, 24, 10));

            // Total Arenas (Fila 31 Excel -> index 30)
            report.setTotalArenasGuardiaA(getCellDouble(sheet, 30, 4));
            report.setTotalArenasGuardiaB(getCellDouble(sheet, 30, 5));
            report.setTotalArenasDia(getCellDouble(sheet, 30, 6));
            report.setTotalArenasPlanDia(getCellDouble(sheet, 30, 7));
            report.setTotalArenasRealAcumMes(getCellDouble(sheet, 30, 8));
            report.setTotalArenasPlanAcumMes(getCellDouble(sheet, 30, 9));

            // Preparación de Cal & pH (Filas 34, 35, 36, 37 -> index 33, 34, 35, 36)
            report.setLechadasPreparadas(getCellInteger(sheet, 33, 4));
            report.setPhPuntoDilucion(getCellDouble(sheet, 34, 4));
            report.setPhLagunaBarcazas(getCellDouble(sheet, 35, 4));
            report.setPhPf4(getCellDouble(sheet, 36, 4));

            // Espesador de Lamas (Filas 38-43 -> index 37-42)
            report.setHidrociclonesNido1(getCellInteger(sheet, 37, 4));
            report.setHidrociclonesNido2(getCellInteger(sheet, 38, 4));
            report.setCaudalAguaRecuperadaM3h(getCellDouble(sheet, 39, 4));
            report.setUfEspesadorPct(getCellDouble(sheet, 40, 4));
            report.setTurbidezFnu(getCellDouble(sheet, 41, 4));
            report.setCaudalNeutralizacionM3h(getCellDouble(sheet, 42, 4));

            // Equipos (Filas 45, 46, 48, 50 -> index 44, 45, 47, 49)
            report.setTractoresOperativosA(getCellInteger(sheet, 44, 4));
            report.setTractoresOperativosB(getCellInteger(sheet, 44, 5));
            report.setUtilizacionTractoresPct(parsePercentage(sheet, 45, 6, formatter));
            report.setUtilizacionCargador994kPct(parsePercentage(sheet, 47, 6, formatter));
            report.setProduccionVolqueteKomatsuTm(getCellDouble(sheet, 49, 6));

            // Asistencia Turno A y B (Filas 99, 101 -> index 98, 100)
            report.setAsistenciaTurnoA(getCellString(sheet, 98, 0, formatter));
            report.setAsistenciaTurnoB(getCellString(sheet, 100, 0, formatter));

            // Novedades Equipos (Fila 93 -> index 92)
            report.setNovedadesEquipos(getCellString(sheet, 92, 0, formatter));

            return report;
        } catch (Exception e) {
            log.warn("Error parsing daily sheet {}: {}", sheet.getSheetName(), e.getMessage());
            return null;
        }
    }

    private List<SapNotice> parseSapNotices(Sheet sheet, DataFormatter formatter, Integer year, Integer month) {
        List<SapNotice> list = new ArrayList<>();
        // Iniciar en fila 4 (index 3)
        for (int r = 3; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String noticeNo = formatter.formatCellValue(row.getCell(2)).trim(); // Col C
            if (noticeNo.isEmpty() || noticeNo.equalsIgnoreCase("Item") || noticeNo.equalsIgnoreCase("Nº Aviso")) {
                continue;
            }

            SapNotice notice = SapNotice.builder()
                    .itemNumber(formatter.formatCellValue(row.getCell(1)).trim())
                    .noticeNumber(noticeNo)
                    .noticeDate(parseCellDate(row.getCell(3)))
                    .equipmentName(formatter.formatCellValue(row.getCell(4)).trim())
                    .description(formatter.formatCellValue(row.getCell(5)).trim())
                    .locationArea(formatter.formatCellValue(row.getCell(6)).trim())
                    .responsibleArea(formatter.formatCellValue(row.getCell(7)).trim())
                    .reporterName(formatter.formatCellValue(row.getCell(8)).trim())
                    .shift(formatter.formatCellValue(row.getCell(9)).trim())
                    .guard(formatter.formatCellValue(row.getCell(10)).trim())
                    .status(formatter.formatCellValue(row.getCell(11)).trim())
                    .resolvedDate(parseCellDate(row.getCell(12)))
                    .delayDays(parseCellInteger(row.getCell(13)))
                    .comments(formatter.formatCellValue(row.getCell(14)).trim())
                    .reportYear(year)
                    .reportMonth(month)
                    .build();

            list.add(notice);
        }
        return list;
    }

    private int parseMonthName(String str) {
        if (str == null) return 0;
        str = str.trim().toLowerCase();
        if (str.startsWith("ene")) return 1;
        if (str.startsWith("feb")) return 2;
        if (str.startsWith("mar")) return 3;
        if (str.startsWith("abr")) return 4;
        if (str.startsWith("may")) return 5;
        if (str.startsWith("jun")) return 6;
        if (str.startsWith("jul")) return 7;
        if (str.startsWith("ago")) return 8;
        if (str.startsWith("sep") || str.startsWith("set")) return 9;
        if (str.startsWith("oct")) return 10;
        if (str.startsWith("nov")) return 11;
        if (str.startsWith("dic")) return 12;
        return 0;
    }

    private LocalDate parseCellDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                double val = cell.getNumericCellValue();
                if (val > 30000 && val < 60000) {
                    Date d = DateUtil.getJavaDate(val);
                    return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
            } else if (cell.getCellType() == CellType.STRING) {
                return tryParseDateString(cell.getStringCellValue());
            } else if (cell.getCellType() == CellType.FORMULA) {
                if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    double val = cell.getNumericCellValue();
                    if (val > 30000 && val < 60000) {
                        Date d = DateUtil.getJavaDate(val);
                        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    }
                } else if (cell.getCachedFormulaResultType() == CellType.STRING) {
                    return tryParseDateString(cell.getStringCellValue());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private LocalDate tryParseDateString(String str) {
        if (str == null || str.isBlank()) return null;
        str = str.trim();

        List<String> patterns = List.of(
                "yyyy/MM/dd", "yyyy/M/d", "yyyy-MM-dd", "yyyy-M-d",
                "M/d/yyyy", "MM/dd/yyyy", "M-d-yyyy", "MM-dd-yyyy",
                "d/M/yyyy", "dd/MM/yyyy", "d-M-yyyy", "dd-MM-yyyy"
        );
        for (String pat : patterns) {
            try {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern(pat));
            } catch (Exception ignored) {}
        }

        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,4})[/\\-](\\d{1,2})[/\\-](\\d{1,4})").matcher(str);
            if (m.find()) {
                int p1 = Integer.parseInt(m.group(1));
                int p2 = Integer.parseInt(m.group(2));
                int p3 = Integer.parseInt(m.group(3));

                int y, mVal, d;
                if (p1 > 1000) {
                    y = p1;
                    if (p2 > 12) { d = p2; mVal = p3; }
                    else { mVal = p2; d = p3; }
                } else {
                    y = p3;
                    if (p1 > 12) { d = p1; mVal = p2; }
                    else if (p2 > 12) { mVal = p1; d = p2; }
                    else {
                        mVal = p1; d = p2;
                    }
                }
                if (y > 1900 && mVal >= 1 && mVal <= 12 && d >= 1 && d <= 31) {
                    return LocalDate.of(y, mVal, d);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private Double getCellDouble(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) return 0.0;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0.0;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.FORMULA) {
                if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    return cell.getNumericCellValue();
                }
            } else if (cell.getCellType() == CellType.STRING) {
                String str = cell.getStringCellValue().replaceAll("[,\\s%]", "").trim();
                return Double.parseDouble(str);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private Integer getCellInteger(Sheet sheet, int rowIndex, int colIndex) {
        Double val = getCellDouble(sheet, rowIndex, colIndex);
        return val != null ? val.intValue() : 0;
    }

    private Integer parseCellInteger(Cell cell) {
        if (cell == null) return 0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return Integer.parseInt(cell.getStringCellValue().trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private Double parsePercentage(Sheet sheet, int rowIndex, int colIndex, DataFormatter formatter) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) return 0.0;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0.0;

        if (cell.getCellType() == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            return val <= 1.0 ? val * 100.0 : val;
        }
        String txt = formatter.formatCellValue(cell).replace("%", "").trim();
        try {
            return Double.parseDouble(txt);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private String getCellString(Sheet sheet, String cellRef, DataFormatter formatter) {
        try {
            org.apache.poi.ss.util.CellReference ref = new org.apache.poi.ss.util.CellReference(cellRef);
            Row row = sheet.getRow(ref.getRow());
            if (row == null) return "";
            Cell cell = row.getCell(ref.getCol());
            return cell == null ? "" : formatter.formatCellValue(cell).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String getCellString(Sheet sheet, int rowIndex, int colIndex, DataFormatter formatter) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) return "";
        Cell cell = row.getCell(colIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }
}
