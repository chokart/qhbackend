package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class DynamicTmsdColumnFinderTest {

    @Test
    public void testFindProductionColumns() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            testSheet(workbook.getSheet("DP"), formatter, "DP");
            testSheet(workbook.getSheet("DL"), formatter, "DL");
        }
    }

    private void testSheet(Sheet sheet, DataFormatter formatter, String name) {
        if (sheet == null) return;

        int prodCol = findProductionColumnIndex(sheet, formatter);
        System.out.printf("\n=== HOJA [%s] -> COLUMNA DE PRODUCCIÓN DETECTADA: %d (%s) ===\n",
                name, prodCol, prodCol >= 0 ? indexToCol(prodCol) : "NO ENCONTRADA");

        if (prodCol >= 0) {
            double sum = 0;
            for (int day = 1; day <= 31; day++) {
                int rA = 9 + (day - 1) * 2;
                int rB = 9 + (day - 1) * 2 + 1;
                double valA = getNumericValue(sheet, rA, prodCol);
                double valB = getNumericValue(sheet, rB, prodCol);
                sum += (valA + valB);
                if (day <= 5) {
                    System.out.printf("Día %02d: Turno A = %-10.1f | Turno B = %-10.1f | Total Día = %.1f\n",
                            day, valA, valB, (valA + valB));
                }
            }
            System.out.printf("TOTAL MES EN [%s] (Col %s): %.1f TM\n", name, indexToCol(prodCol), sum);
        }
    }

    private int findProductionColumnIndex(Sheet sheet, DataFormatter formatter) {
        if (sheet == null) return -1;

        // 1. Buscar coincidencia exacta "TMSD"
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

        // 2. Buscar celdas "TMS" o "TMS2800" o "TMSH"
        for (int r = 0; r <= 15; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                String val = formatter.formatCellValue(cell).trim().replaceAll("\\s+", "");
                if (val.equalsIgnoreCase("TMS") || val.equalsIgnoreCase("TMS2800") || val.equalsIgnoreCase("TMSH")) {
                    return c;
                }
            }
        }

        return -1;
    }

    private double getNumericValue(Sheet sheet, int r, int c) {
        if (sheet == null || r > sheet.getLastRowNum()) return 0.0;
        Row row = sheet.getRow(r);
        if (row == null) return 0.0;
        Cell cell = row.getCell(c);
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
