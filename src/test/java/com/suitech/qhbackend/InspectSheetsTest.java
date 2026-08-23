package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class InspectSheetsTest {

    @Test
    public void inspectDpAndDlSheets() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            inspectSheet(workbook.getSheet("DP"), formatter, "DP");
            inspectSheet(workbook.getSheet("DL"), formatter, "DL");
        }
    }

    private void inspectSheet(Sheet sheet, DataFormatter formatter, String name) {
        if (sheet == null) {
            System.out.println("\n=== HOJA [" + name + "] NO ENCONTRADA ===");
            return;
        }

        System.out.println("\n=======================================================");
        System.out.println("=== HOJA [" + name + "] (CELDAS Y VALORES) ===");
        System.out.println("=======================================================");

        // Imprimir filas 1 a 15 (Encabezados)
        for (int r = 0; r < 15; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Fila %02d: ", r + 1));
            for (int c = 0; c < 20; c++) {
                Cell cell = row.getCell(c);
                String val = getDirectValue(cell, formatter);
                if (!val.isEmpty()) {
                    sb.append(String.format("[%d:%s] ", c, val));
                }
            }
            if (sb.length() > 10) {
                System.out.println(sb.toString());
            }
        }

        // Imprimir filas de datos 16 a 40 (Días del mes y turnos)
        System.out.println("\n--- REGISTROS DE PRODUCCIÓN EN " + name + " (Filas 16-40) ---");
        for (int r = 15; r < 40; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Fila %02d: ", r + 1));
            for (int c = 0; c < 15; c++) {
                Cell cell = row.getCell(c);
                String val = getDirectValue(cell, formatter);
                if (!val.isEmpty()) {
                    sb.append(String.format("[%s] ", val));
                }
            }
            if (sb.length() > 10) {
                System.out.println(sb.toString());
            }
        }
    }

    private String getDirectValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return String.format("%.0f", cell.getNumericCellValue());
                case STRING:
                    return cell.getStringCellValue().trim();
                case FORMULA:
                    try {
                        return String.format("%.0f", cell.getNumericCellValue());
                    } catch (Exception e) {
                        try {
                            return cell.getStringCellValue().trim();
                        } catch (Exception ex) {
                            return formatter.formatCellValue(cell).trim();
                        }
                    }
                default:
                    return formatter.formatCellValue(cell).trim();
            }
        } catch (Exception e) {
            return formatter.formatCellValue(cell).trim();
        }
    }
}
