package com.suitech.qhbackend;

import com.suitech.qhbackend.service.PastedReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AnnualReportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PastedReportService pastedReportService;

    @Test
    public void testAnnualReportEndpoint() throws Exception {
        // Insertar datos de prueba para Enero 2026
        String rawText = "DIA\tTURNO\tDP\tDL\n" +
                "1-Jan-26\tA\t20,962\t3,550\n" +
                "1-Jan-26\tB\t22,198\t2,989\n" +
                "2-Jan-26\tA\t20,961\t1,495\n" +
                "2-Jan-26\tB\t21,091\t2,989";
        pastedReportService.parseAndSavePastedReport(rawText);

        // Consultar el reporte anual /api/v1/reports/annual?year=2026
        mockMvc.perform(get("/api/v1/reports/annual?year=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.months").isArray())
                .andExpect(jsonPath("$.grandDpTotal").exists())
                .andExpect(jsonPath("$.grandDlTotal").exists())
                .andExpect(jsonPath("$.grandTotalYear").exists());

        System.out.println("=== PRUEBA DE REPORTE ANUAL EXITOSA ===");
    }
}
