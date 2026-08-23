package com.interviewiq.service;

/**
 * Extracts plain text from an uploaded resume PDF using Apache PDFBox.
 * Implementations must handle PDFs that fail to extract cleanly (image-only
 * scans, corrupted files) by throwing PdfExtractionException rather than
 * returning empty/garbled text silently.
 */
public interface PdfExtractionService {

    /**
     * @param pdfBytes raw bytes of the uploaded PDF
     * @return cleaned, whitespace-normalized text content
     */
    String extractText(byte[] pdfBytes);
}
