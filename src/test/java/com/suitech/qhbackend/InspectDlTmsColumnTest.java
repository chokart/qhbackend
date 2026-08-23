package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class InspectDlTmsColumnTest {

    @Test
    public void testFindDlTmsColumn() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet dlSheet = workbook.getSheet("DL");
            if (dlSheet == null) return;

            System.out.println("\n=======================================================");
            System.out.println("=== INSPECCIONANDO HOJA [DL] EN COLUMNA 'TMS' (3766, 8125, 12291) ===");
            System.out.println("=======================================================");

            // Buscar en todas las columnas de las filas 5 a 10 cualquier celda que diga 'TMS'
            for (int r = 4; r <= 9; r++) {
                Row row = dlSheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    String val = formatter.formatCellValue(cell).trim();
                    if (val.equalsIgnoreCase("TMS") || val.equalsIgnoreCase("TMSD") || val.contains("2800+L4")) {
                        String colLetter = indexToCol(c);
                        System.out.printf("Fila %02d | Col %d (%s) = [%s]\n", r + 1, c, colLetter, val);
                    }
                }
            }

            // Probar extraccion en filas 10, 11, 12 (Día 1 A, B, Día 2 A) para todas las columnas de la hoja
            System.out.println("\n--- BUSCANDO VALORES 3766, 8125, 12291 EN FILAS 10, 11, 12 ---");
            Row r10 = dlSheet.getRow(9);
            Row r11 = dlSheet.getRow(10);
            Row r12 = dlSheet.getRow(11);

            for (int c = 0; c < r10.getLastCellNum(); c++) {
                double v10 = getNumericValue(r10.getCell(c));
                double v11 = getNumericValue(r11.getCell(c));
                double v12 = getNumericValue(r12.getCell(c));

                if (Math.round(v10) == 3766 || Math.round(v11) == 8125 || Math.round(v12) == 12291) {
                    System.out.printf("¡¡¡ COINCIDENCIA ENCONTRADA EN COLUMNA %d (%s) !!! -> Row10(Fila A)=%.1f, Row11(Fila B)=%.1f, Row12(Fila 2A)=%.1f\n",
                            c, indexToCol(c), v10, v11, v12);
                }
            }
        }
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
