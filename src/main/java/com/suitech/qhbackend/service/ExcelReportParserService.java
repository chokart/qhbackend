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

            int parsedDaysCount = 0;
            Integer detectedYear = null;
            Integer detectedMonth = null;

            // 1. Recorrer hojas que sean partes diarios ("01" a "31")
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String name = sheet.getSheetName().trim();

                if (name.matches("^\\d{2}$")) {
                    DailyReport report = parseDailySheet(sheet, formatter);
                    if (report != null && report.getReportDate() != null) {
                        dailyReportRepository.findByReportDate(report.getReportDate())
                                .ifPresent(existing -> report.setId(existing.getId()));

                        dailyReportRepository.save(report);
                        parsedDaysCount++;

                        if (detectedYear == null) {
                            detectedYear = report.getYearNumber();
                            detectedMonth = report.getMonthNumber();
                        }
                    }
                }
            }

            // 2. Recorrer hoja "Registro aviso SAP" si existe
            int sapCount = 0;
            Sheet sapSheet = workbook.getSheet("Registro aviso SAP");
            if (sapSheet != null && detectedYear != null && detectedMonth != null) {
                sapNoticeRepository.deleteByReportYearAndReportMonth(detectedYear, detectedMonth);
                List<SapNotice> notices = parseSapNotices(sapSheet, formatter, detectedYear, detectedMonth);
                sapNoticeRepository.saveAll(notices);
                sapCount = notices.size();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("daysProcessed", parsedDaysCount);
            result.put("year", detectedYear);
            result.put("month", detectedMonth);
            result.put("sapNoticesProcessed", sapCount);
            result.put("message", "Reporte Excel procesado exitosamente.");
            return result;
        }
    }

    private DailyReport parseDailySheet(Sheet sheet, DataFormatter formatter) {
        try {
            // Fecha en G3
            String dateStr = getCellString(sheet, "G3", formatter);
            LocalDate reportDate = parseLocalDate(sheet, 2, 6, formatter); // Fila index 2 (Fila 3 Excel), Col 6 (G)
            if (reportDate == null && dateStr != null) {
                reportDate = tryParseDateString(dateStr);
            }
            if (reportDate == null) {
                return null;
            }

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
            log.warn("Error parsing sheet {}: {}", sheet.getSheetName(), e.getMessage());
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

    private LocalDate parseLocalDate(Sheet sheet, int rowIndex, int colIndex, DataFormatter formatter) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) return null;
        Cell cell = row.getCell(colIndex);
        return parseCellDate(cell);
    }

    private LocalDate parseCellDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private LocalDate tryParseDateString(String str) {
        if (str == null) return null;
        str = str.replace('/', '-').trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDate.parse(str, fmt);
            } catch (Exception ignored) {}
        }
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
