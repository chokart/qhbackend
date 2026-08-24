package com.suitech.qhbackend;

import com.suitech.qhbackend.model.DailyReport;
import com.suitech.qhbackend.repository.DailyReportRepository;
import com.suitech.qhbackend.service.PastedReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PastedReportIntegrationTest {

    @Autowired
    private PastedReportService pastedReportService;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Test
    public void testParseAndSavePastedReport() {
        String rawText = "DIA\tTURNO\tDP\tDL\n" +
                "1-Jan-26\tA\t20,962\t3,550\n" +
                "1-Jan-26\tB\t22,198\t2,989\n" +
                "2-Jan-26\tA\t20,961\t1,495\n" +
                "2-Jan-26\tB\t21,091\t2,989\n" +
                "31-Jan-26\tA\t15,210\t4,859\n" +
                "31-Jan-26\tB\t22,191\t5,060";

        Map<String, Object> result = pastedReportService.parseAndSavePastedReport(rawText);
        assertNotNull(result);
        assertEquals(2026, result.get("year"));
        assertEquals(1, result.get("month"));

        List<DailyReport> reports = dailyReportRepository.findByYearNumberAndMonthNumberOrderByDayNumberAsc(2026, 1);
        assertEquals(31, reports.size(), "Debe haber 31 días creados/actualizados para enero 2026.");

        DailyReport day1 = reports.get(0);
        assertEquals(20962.0, day1.getDpArenasGuardiaA());
        assertEquals(22198.0, day1.getDpArenasGuardiaB());
        assertEquals(43160.0, day1.getDpArenasTotalDia());

        assertEquals(3550.0, day1.getDlArenasGuardiaA());
        assertEquals(2989.0, day1.getDlArenasGuardiaB());
        assertEquals(6539.0, day1.getDlArenasTotalDia());

        assertEquals(49699.0, day1.getTotalArenasDia());

        System.out.println("=== PRUEBA DE INTEGRACION PASTED REPORT EXITOSA ===");
        System.out.println(result);
    }
}
