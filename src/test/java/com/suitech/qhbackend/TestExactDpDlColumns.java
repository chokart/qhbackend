package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class TestExactDpDlColumns {

    @Test
    public void testDpDlExactExtraction() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet dpSheet = workbook.getSheet("DP");
            Sheet dlSheet = workbook.getSheet("DL");

            System.out.println("\n=== DEGLOSE DIA POR DIA EXTRAYENDO DE HOJAS DP Y DL ===");

            double totalDpMonth = 0;
            double totalDlMonth = 0;

            for (int day = 1; day <= 31; day++) {
                int rowIdxA = 9 + (day - 1) * 2;     // Fila Turno A
                int rowIdxB = 9 + (day - 1) * 2 + 1; // Fila Turno B

                double dpA = extractProdFromRow(dpSheet, rowIdxA);
                double dpB = extractProdFromRow(dpSheet, rowIdxB);

                double dlA = extractProdFromRow(dlSheet, rowIdxA);
                double dlB = extractProdFromRow(dlSheet, rowIdxB);

                double dpDia = dpA + dpB;
                double dlDia = dlA + dlB;
                double totalDia = dpDia + dlDia;

                totalDpMonth += dpDia;
                totalDlMonth += dlDia;

                System.out.printf("Día %02d: DP [A=%-8.1f B=%-8.1f Tot=%-9.1f] | DL [A=%-8.1f B=%-8.1f Tot=%-9.1f] | Total Día=%.1f\n",
                        day, dpA, dpB, dpDia, dlA, dlB, dlDia, totalDia);
            }

            System.out.println("=========================================================================================");
            System.out.printf("TOTAL MES DEPUES DE PARSEAR HOJAS DP Y DL: DP=%.1f | DL=%.1f | GRAN TOTAL=%.1f TM\n",
                    totalDpMonth, totalDlMonth, (totalDpMonth + totalDlMonth));
            System.out.println("=========================================================================================");
        }
    }

    private double extractProdFromRow(Sheet sheet, int rowIdx) {
        if (sheet == null || rowIdx > sheet.getLastRowNum()) return 0.0;
        Row row = sheet.getRow(rowIdx);
        if (row == null) return 0.0;

        double c8 = getNumericValue(row.getCell(8));
        double c16 = getNumericValue(row.getCell(16));
        double c30 = getNumericValue(row.getCell(30));
        double c32 = getNumericValue(row.getCell(32));
        double c43 = getNumericValue(row.getCell(43));
        double c45 = getNumericValue(row.getCell(45));

        // Si existen los valores directos de enviado al área 2800 / underflow
        double sum8_16 = c8 + c16;
        if (sum8_16 > 0) return sum8_16;

        double sum30_32 = c30 + c32;
        if (sum30_32 > 0) return sum30_32;

        if (c45 > 0) return c45;
        return c43;
    }

    private double getNumericValue(Cell cell) {
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
