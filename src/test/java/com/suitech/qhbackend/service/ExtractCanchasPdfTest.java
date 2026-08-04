package com.suitech.qhbackend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ExtractCanchasPdfTest {

    @Test
    public void extractCanchasPdfText() throws Exception {
        File file = new File("D:/qhrelavera/canchas.pdf");
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            System.out.println("=== CANCHAS.PDF TEXT START ===");
            System.out.println(text);
            System.out.println("=== CANCHAS.PDF TEXT END ===");
        }
    }
}
