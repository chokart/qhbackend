package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class FindTmsdInDlTest {

    @Test
    public void testFindTmsdInDl() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet dlSheet = workbook.getSheet("DL");
            if (dlSheet == null) return;

            System.out.println("\n=======================================================");
            System.out.println("=== BUSCANDO COLUMNAS CON 'TMS' EN HOJA [DL] ===");
            System.out.println("=======================================================");

            for (int r = 0; r <= 10; r++) {
                Row row = dlSheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    String val = formatter.formatCellValue(cell).trim();
                    if (val.toUpperCase().contains("TMS") || val.toUpperCase().contains("PRODUCCION") || val.toUpperCase().contains("TOTAL")) {
                        String colLetter = indexToCol(c);
                        System.out.printf("DL Fila %02d | Col %d (%s) = [%s]\n", r + 1, c, colLetter, val);
                    }
                }
            }

            // Probar valores de las columnas 43 (AR), 44 (AS), 45 (AT), 51 (AZ), 53 (BB), 101 (CX), 113 (DJ) en DL para los dias 1 a 5
            int[] testCols = {43, 44, 45, 51, 53, 101, 113};
            System.out.println("\n--- VALORES EN HOJA DL PARA DÍAS 1 AL 5 ---");
            for (int day = 1; day <= 5; day++) {
                int rA = 9 + (day - 1) * 2;
                int rB = 9 + (day - 1) * 2 + 1;
                System.out.printf("Día %02d (Fila A=%d, B=%d):\n", day, rA + 1, rB + 1);
                for (int c : testCols) {
                    double vA = getNumericValue(dlSheet, rA, c);
                    double vB = getNumericValue(dlSheet, rB, c);
                    System.out.printf("  Col %d (%s): Turno A=%.1f | Turno B=%.1f | Total Día=%.1f\n",
                            c, indexToCol(c), vA, vB, (vA + vB));
                }
            }
        }
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
