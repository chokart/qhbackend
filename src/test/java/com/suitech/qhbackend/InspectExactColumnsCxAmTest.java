package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class InspectExactColumnsCxAmTest {

    @Test
    public void testInspectCxAndAmColumns() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            inspectSheetColCxAm(workbook.getSheet("DL"), formatter, "DL");
            inspectSheetColCxAm(workbook.getSheet("DP"), formatter, "DP");
        }
    }

    private void inspectSheetColCxAm(Sheet sheet, DataFormatter formatter, String name) {
        if (sheet == null) return;
        System.out.println("\n=======================================================");
        System.out.println("=== INSPECCIONANDO HOJA [" + name + "] EN COLUMNAS AM Y CX ===");
        System.out.println("=======================================================");

        int colAM = colToIndex("AM"); // Parada DL
        int colCX = colToIndex("CX"); // Producción DL

        System.out.printf("Indice Col AM = %d | Indice Col CX = %d\n", colAM, colCX);

        // Encabezados en Filas 1 a 9
        for (int r = 0; r < 9; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String valAM = formatter.formatCellValue(row.getCell(colAM)).trim();
            String valCX = formatter.formatCellValue(row.getCell(colCX)).trim();
            if (!valAM.isEmpty() || !valCX.isEmpty()) {
                System.out.printf("Encabezado Fila %02d: AM (col %d)=[%s] | CX (col %d)=[%s]\n", r + 1, colAM, valAM, colCX, valCX);
            }
        }

        // Filas de datos (Días 1 a 15 -> Filas 10 a 40)
        System.out.println("\n--- PRIMEROS 20 REGISTROS (DÍAS 1 AL 10, TURNOS A Y B) ---");
        for (int r = 9; r < 30; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String fecha = formatter.formatCellValue(row.getCell(0)).trim();
            String turno = formatter.formatCellValue(row.getCell(1)).trim();

            String valAM = getNumericOrString(row.getCell(colAM));
            String valCX = getNumericOrString(row.getCell(colCX));

            System.out.printf("Fila %02d [Fecha=%-8s Turno=%-2s]: Parada AM(col %d)=%-10s | Producción CX(col %d)=%-12s\n",
                    r + 1, fecha, turno, colAM, valAM, colCX, valCX);
        }
    }

    private int colToIndex(String colStr) {
        int idx = 0;
        for (int i = 0; i < colStr.length(); i++) {
            idx = idx * 26 + (colStr.charAt(i) - 'A' + 1);
        }
        return idx - 1;
    }

    private String getNumericOrString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    double d = cell.getNumericCellValue();
                    if (d == (long) d) return String.valueOf((long) d);
                    return String.format("%.2f", d);
                case STRING:
                    return cell.getStringCellValue().trim();
                case FORMULA:
                    try {
                        double fd = cell.getNumericCellValue();
                        if (fd == (long) fd) return String.valueOf((long) fd);
                        return String.format("%.2f", fd);
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
