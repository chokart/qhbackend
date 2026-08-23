package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class DpDlParserStrategyTest {

    @Test
    public void testDpDlDirectParsing() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet dpSheet = workbook.getSheet("DP");
            Sheet dlSheet = workbook.getSheet("DL");

            System.out.println("=== PROBANDO PARSEO DIRECTO DE DP Y DL ===");
            for (int day = 1; day <= 31; day++) {
                int rowIdxA = 9 + (day - 1) * 2;     // Fila Turno A (ej. Day 1 -> Fila 10, index 9)
                int rowIdxB = 9 + (day - 1) * 2 + 1; // Fila Turno B (ej. Day 1 -> Fila 11, index 10)

                double dpA = getRowProduction(dpSheet, rowIdxA);
                double dpB = getRowProduction(dpSheet, rowIdxB);

                double dlA = getRowProduction(dlSheet, rowIdxA);
                double dlB = getRowProduction(dlSheet, rowIdxB);

                if (dpA > 0 || dpB > 0 || dlA > 0 || dlB > 0) {
                    System.out.printf("Día %02d: DP A=%-8.1f | DP B=%-8.1f | DL A=%-8.1f | DL B=%-8.1f | Total Día=%.1f\n",
                            day, dpA, dpB, dlA, dlB, (dpA + dpB + dlA + dlB));
                }
            }
        }
    }

    private double getRowProduction(Sheet sheet, int rowIdx) {
        if (sheet == null || rowIdx > sheet.getLastRowNum()) return 0.0;
        Row row = sheet.getRow(rowIdx);
        if (row == null) return 0.0;

        // Probar celdas de Underflow (Col 30 y Col 32) y Col 43/45/51
        double col30 = getCellValue(row.getCell(30));
        double col32 = getCellValue(row.getCell(32));
        double col43 = getCellValue(row.getCell(43));
        double col45 = getCellValue(row.getCell(45));
        double col51 = getCellValue(row.getCell(51));

        if ((col30 + col32) > 0) return col30 + col32;
        if (col45 > 0) return col45;
        if (col43 > 0) return col43;
        return col51;
    }

    private double getCellValue(Cell cell) {
        if (cell == null) return 0.0;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case FORMULA:
                    try { return cell.getNumericCellValue(); } catch (Exception e) { return 0.0; }
                case STRING:
                    try { return Double.parseDouble(cell.getStringCellValue().trim().replace(",", "")); } catch (Exception e) { return 0.0; }
                default:
                    return 0.0;
            }
        } catch (Exception e) {
            return 0.0;
        }
    }
}
