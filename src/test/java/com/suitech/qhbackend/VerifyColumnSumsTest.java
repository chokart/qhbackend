package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class VerifyColumnSumsTest {

    @Test
    public void testColumnsSum() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            verifySheetSums(workbook.getSheet("DP"), "DP");
            verifySheetSums(workbook.getSheet("DL"), "DL");
        }
    }

    private void verifySheetSums(Sheet sheet, String name) {
        if (sheet == null) return;
        System.out.println("\n=======================================================");
        System.out.println("=== COMPROBANDO SUMATORIA DE COLUMNAS EN [" + name + "] ===");
        System.out.println("=======================================================");

        double sumC8 = 0, sumC16 = 0, sumC30 = 0, sumC32 = 0, sumC43 = 0, sumC44 = 0, sumC45 = 0, sumC51 = 0, sumC53 = 0;

        for (int r = 9; r <= 75; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            sumC8 += getNumeric(row.getCell(8));
            sumC16 += getNumeric(row.getCell(16));
            sumC30 += getNumeric(row.getCell(30));
            sumC32 += getNumeric(row.getCell(32));
            sumC43 += getNumeric(row.getCell(43));
            sumC44 += getNumeric(row.getCell(44));
            sumC45 += getNumeric(row.getCell(45));
            sumC51 += getNumeric(row.getCell(51));
            sumC53 += getNumeric(row.getCell(53));
        }

        System.out.printf("Sum Col 8  (Relave 1): %.2f\n", sumC8);
        System.out.printf("Sum Col 16 (Relave 2): %.2f\n", sumC16);
        System.out.printf("Sum Col 30 (Underflow 1): %.2f\n", sumC30);
        System.out.printf("Sum Col 32 (Underflow 2): %.2f\n", sumC32);
        System.out.printf("Sum Col 43: %.2f\n", sumC43);
        System.out.printf("Sum Col 44: %.2f\n", sumC44);
        System.out.printf("Sum Col 45: %.2f\n", sumC45);
        System.out.printf("Sum Col 51: %.2f\n", sumC51);
        System.out.printf("Sum Col 53: %.2f\n", sumC53);
    }

    private double getNumeric(Cell cell) {
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
