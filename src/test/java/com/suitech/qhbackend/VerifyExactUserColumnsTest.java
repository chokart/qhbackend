package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class VerifyExactUserColumnsTest {

    @Test
    public void testUserColumns() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet dpSheet = workbook.getSheet("DP");
            Sheet dlSheet = workbook.getSheet("DL");

            System.out.println("\n=======================================================");
            System.out.println("=== VERIFICANDO COLUMNA CX (PRODUCCIÓN) Y AM (PARADAS) ===");
            System.out.println("=======================================================");

            int colAM = 38;  // AM (0-based: 38)
            int colCX = 101; // CX (0-based: 101)

            double totDp = 0;
            double totDl = 0;

            for (int day = 1; day <= 31; day++) {
                int rA = 9 + (day - 1) * 2;
                int rB = 9 + (day - 1) * 2 + 1;

                double dpA_cx = getVal(dpSheet, rA, colCX);
                double dpB_cx = getVal(dpSheet, rB, colCX);
                double dpA_am = getVal(dpSheet, rA, colAM);
                double dpB_am = getVal(dpSheet, rB, colAM);

                double dlA_cx = getVal(dlSheet, rA, colCX);
                double dlB_cx = getVal(dlSheet, rB, colCX);
                double dlA_am = getVal(dlSheet, rA, colAM);
                double dlB_am = getVal(dlSheet, rB, colAM);

                totDp += (dpA_cx + dpB_cx);
                totDl += (dlA_cx + dlB_cx);

                System.out.printf("Día %02d | DP: Turno A=%.1f (Parada: %.2f hr), Turno B=%.1f (Parada: %.2f hr) || DL: Turno A=%.1f (Parada: %.2f hr), Turno B=%.1f (Parada: %.2f hr)\n",
                        day, dpA_cx, dpA_am, dpB_cx, dpB_am, dlA_cx, dlA_am, dlB_cx, dlB_am);
            }

            System.out.println("--------------------------------------------------------------------------------------");
            System.out.printf("TOTAL MES CX (PRODUCCIÓN): DP=%.1f TM | DL=%.1f TM | GRAN TOTAL=%.1f TM\n",
                    totDp, totDl, (totDp + totDl));
            System.out.println("--------------------------------------------------------------------------------------");
        }
    }

    private double getVal(Sheet sheet, int r, int c) {
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
}
