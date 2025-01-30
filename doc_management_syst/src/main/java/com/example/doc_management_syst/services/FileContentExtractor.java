package com.example.doc_management_syst.services;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileContentExtractor {

    public static String extractFileContent(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".pdf")) {
            return extractPdfContent(filePath.toFile());
        } else if (fileName.endsWith(".docx")) {
            return extractDocxContent(filePath.toFile());
        } else if (fileName.endsWith(".txt")) {
            return extractTxtContent(filePath);
        } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return extractImageContent(filePath.toFile());
        } else {
            throw new IOException("Unsupported file format: " + fileName);
        }
    }
    private static String extractImageContent(File imageFile) throws IOException {
        Tesseract tesseract = new Tesseract();

        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("eng");

        try {
            return tesseract.doOCR(imageFile);
        } catch (TesseractException e) {
            throw new IOException("Error extracting text from image: " + e.getMessage(), e);
        }
    }
    private static String extractPdfContent(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
    private static String extractDocxContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder text = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append("\n"));
            return text.toString();
        }
    }

    private static String extractTxtContent(Path filePath) throws IOException {
        return Files.readString(filePath);
    }
}
