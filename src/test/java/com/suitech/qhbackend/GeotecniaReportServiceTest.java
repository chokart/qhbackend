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

        GeotecniaReportService.ImportReportResult resultPerfil = service.processPerfilBytes(pdfBytes, "TEST_USER");

        System.out.println("=== TEST PERFIL RESULT ===");
        System.out.println("Principal Updated: " + resultPerfil.getPrincipalUpdated());
        for (String msg : resultPerfil.getLogMessages()) {
            System.out.println(msg);
        }

        File canchasFile = new File("D:\\qhrelavera\\canchas.pdf");
        if (canchasFile.exists()) {
            byte[] canchasBytes = Files.readAllBytes(canchasFile.toPath());
            GeotecniaReportService.ImportReportResult resultCanchas = service.processCanchasBytes(canchasBytes, "TEST_USER");
            System.out.println("=== TEST CANCHAS RESULT ===");
            System.out.println("Principal Updated: " + resultCanchas.getPrincipalUpdated());
            System.out.println("Lateral Updated: " + resultCanchas.getLateralUpdated());
            for (String msg : resultCanchas.getLogMessages()) {
                System.out.println(msg);
            }
        }
    }
}
