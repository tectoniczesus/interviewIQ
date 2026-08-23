package com.interviewiq.service.impl;

import com.interviewiq.exception.PdfExtractionException;
import com.interviewiq.service.PdfExtractionService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfBoxExtractionService implements PdfExtractionService {

    @Override
    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new PdfExtractionException("Cannot extract text from an empty file");
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new PdfExtractionException("Cannot process a password-protected PDF");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String rawText = stripper.getText(document);

            String cleaned = cleanWhitespace(rawText);

            if (cleaned.isBlank()) {
                throw new PdfExtractionException(
                        "No extractable text found in this PDF - it may be a scanned image rather than text");
            }

            return cleaned;
        } catch (PdfExtractionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PdfExtractionException("Failed to read the uploaded PDF", ex);
        }
    }

    private String cleanWhitespace(String text) {
        return text
                .replace("\r\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
