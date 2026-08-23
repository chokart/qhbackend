package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@SpringBootTest
public class RowByRowDateTurnoParserTest {

    @Test
    public void testRowByRowParsing() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            testSheetRowByRow(workbook.getSheet("DP"), formatter, "DP");
            testSheetRowByRow(workbook.getSheet("DL"), formatter, "DL");
        }
    }

    private void testSheetRowByRow(Sheet sheet, DataFormatter formatter, String name) {
        if (sheet == null) return;

        System.out.println("\n=======================================================");
        System.out.println("=== VERIFICANDO LECTURA EXACTA DE COLUMNA A (FECHA) Y B (TURNO) EN [" + name + "] ===");
        System.out.println("=======================================================");

        int tmsdCol = findTmsdColumnIndex(sheet, formatter);
        System.out.printf("Hoja [%s] -> Columna TMSD detectada en Indice %d (%s)\n",
                name, tmsdCol, tmsdCol >= 0 ? indexToCol(tmsdCol) : "NO ENCONTRADA");

        int year = 2026;
        int month = 7;
        int currentDayFallback = 1;

        for (int r = 9; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String turnoStr = formatter.formatCellValue(row.getCell(1)).trim();
            if (!turnoStr.equalsIgnoreCase("A") && !turnoStr.equalsIgnoreCase("B")) {
                continue;
            }

            LocalDate rowDate = parseRowDate(row.getCell(0), year, month, currentDayFallback);
            double tmsdVal = tmsdCol >= 0 ? getNumericValue(row.getCell(tmsdCol)) : 0.0;

            System.out.printf("Fila %02d | Fecha Col A = %-10s | Turno Col B = %-2s | TMSD = %.1f\n",
                    r + 1, rowDate.toString(), turnoStr, tmsdVal);

            if (turnoStr.equalsIgnoreCase("B")) {
                currentDayFallback++;
            }
        }
    }

    private LocalDate parseRowDate(Cell cell, int year, int month, int fallbackDay) {
        if (cell == null) return LocalDate.of(year, month, Math.min(fallbackDay, 31));
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                double num = cell.getNumericCellValue();
                if (num > 40000 && num < 60000) {
                    Date date = DateUtil.getJavaDate(num);
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                if (num >= 1 && num <= 31) {
                    return LocalDate.of(year, month, (int) num);
                }
            }
            if (cell.getCellType() == CellType.FORMULA) {
                try {
                    double num = cell.getNumericCellValue();
                    if (num > 40000 && num < 60000) {
                        Date date = DateUtil.getJavaDate(num);
                        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    }
                    if (num >= 1 && num <= 31) {
                        return LocalDate.of(year, month, (int) num);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return LocalDate.of(year, month, Math.min(fallbackDay, 31));
    }

    private int findTmsdColumnIndex(Sheet sheet, DataFormatter formatter) {
        if (sheet == null) return -1;
        for (int r = 0; r <= 15; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                String val = formatter.formatCellValue(cell).trim().replaceAll("\\s+", "");
                if (val.equalsIgnoreCase("TMSD")) {
                    return c;
                }
            }
        }
        for (int r = 0; r <= 15; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                String val = formatter.formatCellValue(cell).trim().replaceAll("\\s+", "");
                if (val.equalsIgnoreCase("TMS2800") || val.equalsIgnoreCase("TMS")) {
                    return c;
                }
            }
        }
        return -1;
    }

    private double getNumericValue(Cell cell) {
        if (cell == null) return 0.0;
        try {
            switch (cell.getCellType()) {
                case NUMERIC: return cell.getNumericCellValue();
                case FORMULA:
                    try { return cell.getNumericCellValue(); } catch (Exception e) { return 0.0; }
                case STRING:
                    try { return Double.parseDouble(cell.getStringCellValue().trim().replace(",", "")); } catch (Exception e) { return 0.0; }
                default: return 0.0;
            }
        } catch (Exception e) { return 0.0; }
    }

    private String indexToCol(int idx) {
        StringBuilder sb = new StringBuilder();
        idx++;
        while (idx > 0) {
            int rem = (idx - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            idx = (idx - 1) / 26;
        }
        return sb.toString();
    }
}
