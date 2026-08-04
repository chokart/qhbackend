package com.suitech.qhbackend;

import com.suitech.qhbackend.model.Cancha;
import com.suitech.qhbackend.model.CanchaCapa;
import com.suitech.qhbackend.repository.CanchaCapaRepository;
import com.suitech.qhbackend.repository.CanchaRepository;
import com.suitech.qhbackend.service.GeotecniaReportService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class GeotecniaReportServiceTest {

    @Test
    public void testProcessPerfilPdf() throws Exception {
        CanchaRepository canchaRepository = Mockito.mock(CanchaRepository.class);
        CanchaCapaRepository canchaCapaRepository = Mockito.mock(CanchaCapaRepository.class);

        when(canchaRepository.findByNumber(anyInt())).thenAnswer(inv -> {
            int num = inv.getArgument(0);
            return Optional.of(Cancha.builder().number(num).currentHeight(1100.0).build());
        });

        when(canchaCapaRepository.findByNumber(anyInt())).thenAnswer(inv -> {
            int num = inv.getArgument(0);
            return Optional.of(CanchaCapa.builder().number(num).currentCapa(1).build());
        });

        GeotecniaReportService service = new GeotecniaReportService(canchaRepository, canchaCapaRepository);

        File pdfFile = new File("D:\\qhrelavera\\perfil.pdf");
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());

        GeotecniaReportService.ImportReportResult result = service.processPdfBytes(pdfBytes, "TEST_USER");

        System.out.println("=== TEST IMPORT RESULT ===");
        System.out.println("Principal Updated: " + result.getPrincipalUpdated());
        System.out.println("Lateral Updated: " + result.getLateralUpdated());
        for (String msg : result.getLogMessages()) {
            System.out.println(msg);
        }
    }
}
