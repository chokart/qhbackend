package com.suitech.qhbackend.service;

import com.suitech.qhbackend.model.Cancha;
import com.suitech.qhbackend.model.CanchaCapa;
import com.suitech.qhbackend.repository.CanchaCapaRepository;
import com.suitech.qhbackend.repository.CanchaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GeotecniaReportServiceTest {

    @Mock
    private CanchaRepository canchaRepository;

    @Mock
    private CanchaCapaRepository canchaCapaRepository;

    @InjectMocks
    private GeotecniaReportService service;

    @BeforeEach
    public void setup() {
        lenient().when(canchaRepository.findByNumber(anyInt()))
                .thenAnswer(invocation -> {
                    int num = invocation.getArgument(0);
                    return Optional.of(Cancha.builder().number(num).currentHeight(1050.0).build());
                });

        lenient().when(canchaCapaRepository.findByNumber(anyInt()))
                .thenAnswer(invocation -> {
                    int num = invocation.getArgument(0);
                    return Optional.of(CanchaCapa.builder().number(num).currentCapa(1).build());
                });
    }

    @Test
    public void testProcessPerfilPdf() throws Exception {
        File perfilFile = new File("D:/qhrelavera/perfil.pdf");
        assertTrue(perfilFile.exists(), "perfil.pdf debe existir para la prueba");

        byte[] bytes = Files.readAllBytes(perfilFile.toPath());
        GeotecniaReportService.ImportReportResult result = service.processPerfilBytes(bytes, "TEST_USER");

        assertNotNull(result);
        assertTrue(result.getPrincipalUpdated() > 0, "Debe actualizar al menos una cancha en el Dique Principal");
        System.out.println("Log Perfil: " + result.getLogMessages());
    }

    @Test
    public void testProcessCanchasPdf() throws Exception {
        File canchasFile = new File("D:/qhrelavera/canchas.pdf");
        assertTrue(canchasFile.exists(), "canchas.pdf debe existir para la prueba");

        byte[] bytes = Files.readAllBytes(canchasFile.toPath());
        GeotecniaReportService.ImportReportResult result = service.processCanchasBytes(bytes, "TEST_USER");

        assertNotNull(result);
        assertTrue(result.getPrincipalUpdated() > 0, "Debe actualizar canchas en Dique Principal");
        assertTrue(result.getLateralUpdated() > 0, "Debe actualizar canchas en Dique Lateral");
        System.out.println("Log Canchas: " + result.getLogMessages());
    }
}
