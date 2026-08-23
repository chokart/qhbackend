package com.suitech.qhbackend;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class DetailedDpDlInspectorTest {

    @Test
    public void inspectDpDlDetailedColumns() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            inspectSheetStructure(workbook.getSheet("DP"), formatter, "DP");
            inspectSheetStructure(workbook.getSheet("DL"), formatter, "DL");
        }
    }

    private void inspectSheetStructure(Sheet sheet, DataFormatter formatter, String name) {
        if (sheet == null) {
            System.out.println("HOJA " + name + " NO ENCONTRADA");
            return;
        }

        System.out.println("\n=======================================================");
        System.out.println("=== ESTRUCTURA DETALLADA DE LA HOJA [" + name + "] ===");
        System.out.println("=======================================================");

        // Encabezados (Filas 6, 7, 8, 9)
        for (int r = 5; r <= 8; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            System.out.printf("Fila %02d: ", r + 1);
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                String val = formatter.formatCellValue(cell).trim();
                if (!val.isEmpty()) {
                    System.out.printf("Col[%d]=%s | ", c, val);
                }
            }
            System.out.println();
        }

        // Filas de Datos (Filas 10 a 72 -> Días 1 a 31 en Turnos A y B)
        System.out.println("\n--- FILAS DE REGISTROS DE PRODUCCIÓN DE " + name + " (Primeros 10 registros) ---");
        int count = 0;
        for (int r = 9; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String dateVal = formatter.formatCellValue(row.getCell(0)).trim();
            String turnoVal = formatter.formatCellValue(row.getCell(1)).trim();

            if (dateVal.isEmpty() && turnoVal.isEmpty()) continue;

            System.out.printf("Fila %02d: Fecha(Col0)=%-10s | Turno(Col1)=%-2s | ", r + 1, dateVal, turnoVal);
            for (int c = 2; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                String val = formatter.formatCellValue(cell).trim();
                if (!val.isEmpty()) {
                    System.out.printf("Col[%d]=%s | ", c, val);
                }
            }
            System.out.println();
            count++;
            if (count >= 14) break; // Mostrar primeros 14 registros (7 días x 2 turnos)
        }
    }
}
