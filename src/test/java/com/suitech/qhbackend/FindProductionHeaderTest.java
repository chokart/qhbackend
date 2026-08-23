package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class FindProductionHeaderTest {

    @Test
    public void testFindHeaders() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            findHeadersInSheet(workbook.getSheet("DL"), formatter, "DL");
            findHeadersInSheet(workbook.getSheet("DP"), formatter, "DP");
        }
    }

    private void findHeadersInSheet(Sheet sheet, DataFormatter formatter, String name) {
        if (sheet == null) return;
        System.out.println("\n=======================================================");
        System.out.println("=== BUSCANDO ENCABEZADOS DE PRODUCCIÓN Y PARADAS EN [" + name + "] ===");
        System.out.println("=======================================================");

        for (int r = 0; r <= 10; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                String val = formatter.formatCellValue(cell).trim();
                if (val.toLowerCase().contains("producci") || val.toLowerCase().contains("tms") || val.toLowerCase().contains("parada") || val.toLowerCase().contains("real")) {
                    String colLetter = indexToCol(c);
                    System.out.printf("Fila %02d | Col %d (%s) = [%s]\n", r + 1, c, colLetter, val);
                }
            }
        }

        // Probar extracción de valores en primeros 6 registros (Filas 10 a 15) para las columnas encontradas
        System.out.println("\n--- VALORES DE FILAS 10 A 15 EN " + name + " ---");
        for (int r = 9; r <= 14; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String fecha = formatter.formatCellValue(row.getCell(0)).trim();
            String turno = formatter.formatCellValue(row.getCell(1)).trim();

            System.out.printf("Fila %02d [F=%s, T=%s]: ", r + 1, fecha, turno);
            for (int c = 95; c < Math.min(row.getLastCellNum(), 110); c++) {
                Cell cell = row.getCell(c);
                String val = getNumericOrString(cell);
                if (!val.isEmpty()) {
                    System.out.printf("%s(col %d)=%s | ", indexToCol(c), c, val);
                }
            }
            System.out.println();
        }
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

    private String getNumericOrString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
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
                        return "";
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}
