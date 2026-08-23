package com.suitech.qhbackend;

import com.suitech.qhbackend.model.DailyReport;
import com.suitech.qhbackend.repository.DailyReportRepository;
import com.suitech.qhbackend.repository.SapNoticeRepository;
import com.suitech.qhbackend.service.ExcelReportParserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExcelReportParserTest {

    @Autowired
    private ExcelReportParserService parserService;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Test
    public void testParseReporteOperacionesFile() throws Exception {
        File excelFile = new File("D:/qhrelavera/Reporte de Operaciones Quebrada Honda.xlsm");
        assertTrue(excelFile.exists(), "El archivo Reporte de Operaciones Quebrada Honda.xlsm debe existir.");

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    excelFile.getName(),
                    "application/vnd.ms-excel.sheet.macroEnabled.12",
                    fis
            );

            Map<String, Object> result = parserService.parseAndSaveExcelReport(multipartFile);
            System.out.println("=== RESULTADO DEL PARSEO EXCEL ===");
            System.out.println(result);

            Integer year = (Integer) result.get("year");
            Integer month = (Integer) result.get("month");
            Integer daysProcessed = (Integer) result.get("daysProcessed");

            assertNotNull(year);
            assertNotNull(month);
            assertTrue(daysProcessed > 0, "Debe procesar más de 0 días de producción.");

            List<DailyReport> reports = dailyReportRepository
                    .findByYearNumberAndMonthNumberOrderByDayNumberAsc(year, month);

            System.out.println("=== PARTES DIARIOS RECUPERADOS DE LA BASE DE DATOS ===");
            System.out.println("Total días recuperados: " + reports.size());

            double totalDp = 0;
            double totalDl = 0;
            double grandTotal = 0;

            for (DailyReport r : reports) {
                double dpA = r.getDpArenasGuardiaA() != null ? r.getDpArenasGuardiaA() : 0;
                double dpB = r.getDpArenasGuardiaB() != null ? r.getDpArenasGuardiaB() : 0;
                double dpTot = r.getDpArenasTotalDia() != null ? r.getDpArenasTotalDia() : 0;

                double dlA = r.getDlArenasGuardiaA() != null ? r.getDlArenasGuardiaA() : 0;
                double dlB = r.getDlArenasGuardiaB() != null ? r.getDlArenasGuardiaB() : 0;
                double dlTot = r.getDlArenasTotalDia() != null ? r.getDlArenasTotalDia() : 0;

                double dayTot = r.getTotalArenasDia() != null ? r.getTotalArenasDia() : 0;

                totalDp += dpTot;
                totalDl += dlTot;
                grandTotal += dayTot;

                System.out.printf("Día %02d | DP A: %,.0f | DP B: %,.0f | Total DP: %,.0f || DL A: %,.0f | DL B: %,.0f | Total DL: %,.0f || TOTAL DÍA: %,.0f TM%n",
                        r.getDayNumber(), dpA, dpB, dpTot, dlA, dlB, dlTot, dayTot);
            }

            System.out.printf("%n=== RESUMEN TOTAL MES (%d/%d) ===%n", month, year);
            System.out.printf("Total Dique Principal (DP): %,.0f TM%n", totalDp);
            System.out.printf("Total Dique Lateral (DL): %,.0f TM%n", totalDl);
            System.out.printf("Gran Total Producción Arenas: %,.0f TM%n", grandTotal);
        }
    }
}
