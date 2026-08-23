package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class TestDpDlValues {

    @Test
    public void testPrintRowValues() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            printSheetValues(workbook.getSheet("DP"), formatter, "DP");
            printSheetValues(workbook.getSheet("DL"), formatter, "DL");
        }
    }

    private void printSheetValues(Sheet sheet, DataFormatter formatter, String name) {
        if (sheet == null) return;
        System.out.println("\n=======================================================");
        System.out.println("=== VALORES REALES DE " + name + " (Filas 10 a 20) ===");
        System.out.println("=======================================================");

        // Encabezados en Fila 6, 7, 8, 9
        Row hRow = sheet.getRow(8); // Fila 9
        
        for (int r = 9; r <= 22; r++) { // Filas 10 a 23
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String dateCell = getNumericOrString(row.getCell(0));
            String turnoCell = getNumericOrString(row.getCell(1));

            System.out.printf("Fila %02d [Fecha=%s, Turno=%s]: ", r + 1, dateCell, turnoCell);
            for (int c = 2; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                String val = getNumericOrString(cell);
                if (!val.isEmpty() && !val.equals("0")) {
                    String hVal = hRow != null ? getNumericOrString(hRow.getCell(c)) : "";
                    System.out.printf("C%d(%s)=%s | ", c, hVal, val);
                }
            }
            System.out.println();
        }
    }

    private String getNumericOrString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    double d = cell.getNumericCellValue();
                    if (d == (long) d) return String.valueOf((long) d);
                    return String.format("%.1f", d);
                case STRING:
                    return cell.getStringCellValue().trim();
                case FORMULA:
                    try {
                        double fd = cell.getNumericCellValue();
                        if (fd == (long) fd) return String.valueOf((long) fd);
                        return String.format("%.1f", fd);
                    } catch (Exception e) {
                        try {
                            return cell.getStringCellValue().trim();
                        } catch (Exception ex) {
                            return "";
                        }
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}
