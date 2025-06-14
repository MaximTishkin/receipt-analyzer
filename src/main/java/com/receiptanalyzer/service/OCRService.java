package com.receiptanalyzer.service;

import com.receiptanalyzer.model.ReceiptInfo;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Arrays;

@Slf4j
@Service
public class OCRService {
    private final ImagePreprocessor imagePreprocessor;
    private final ReceiptDataExtractor dataExtractor;
    private final TextCorrectionService textCorrectionService;
    private final Tesseract tesseract;

    @Autowired
    public OCRService(ImagePreprocessor imagePreprocessor,
                      ReceiptDataExtractor dataExtractor,
                      TextCorrectionService textCorrectionService,
                      Tesseract tesseract) {
        this.imagePreprocessor = imagePreprocessor;
        this.dataExtractor = dataExtractor;
        this.textCorrectionService = textCorrectionService;
        this.tesseract = tesseract;
    }

    /**
     * Обрабатывает изображение чека и возвращает только структурированные данные.
     */
    public ReceiptInfo processReceipt(BufferedImage image) {
        try {
            log.info("Starting receipt processing with debug info");

            // Предварительная обработка изображения
            BufferedImage preprocessedImage = preprocessImage(image);
            log.debug("Image preprocessing completed");

            // Выполняем OCR
            String rawText = doOcr(preprocessedImage);
            log.debug("OCR processing completed. Raw result: {}", rawText);

            // Очищаем текст
            String cleanedText = postProcessText(rawText);
            log.debug("Text cleaned. Result: {}", cleanedText);

            // Извлекаем структурированные данные
            ReceiptInfo receiptInfo = dataExtractor.extractData(cleanedText);
            log.info("Receipt data extracted: {}", receiptInfo);

            return receiptInfo;

        } catch (Exception e) {
            log.error("Unexpected error during receipt processing: {}", e.getMessage());
            throw new RuntimeException("Failed to process receipt", e);
        }
    }

    /**
     * Предварительная обработка изображения.
     */
    private BufferedImage preprocessImage(BufferedImage image) {
        return imagePreprocessor.preprocessImage(image);
    }

    /**
     * Выполняет OCR над изображением.
     */
    private String doOcr(BufferedImage image) throws Exception {
        return tesseract.doOCR(image);
    }

    /**
     * Постобработка текста после OCR: исправление ошибок, очистка и фильтрация строк.
     */
    private String postProcessText(String text) {
        if (isNullOrEmpty(text)) return "";
        try {
            // Сначала исправляем ошибки в тексте
            String correctedText = textCorrectionService.correctText(text);
            
            // Затем применяем стандартную обработку
            return Arrays.stream(correctedText.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(this::cleanupLine)
                    .filter(this::isValidReceiptLine)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        } catch (Exception e) {
            log.error("Error during text post-processing: {}", e.getMessage());
            return text;
        }
    }

    /**
     * Удаляет множественные пробелы в строке.
     */
    private String cleanupLine(String line) {
        try {
            // Удаляем множественные пробелы
            line = line.replaceAll("\\s+", " ");
            
            return line.trim();
        } catch (Exception e) {
            log.error("Error during line cleanup: {}", e.getMessage());
            return line;
        }
    }
    
    /**
     * Проверяет, содержит ли строка полезную информацию.
     */
    private boolean isValidReceiptLine(String line) {
        try {
            // Проверяем, содержит ли строка полезную информацию
            return line.length() >= 3;
        } catch (Exception e) {
            log.error("Error during line validation: {}", e.getMessage());
            return false;
        }
    }

    private boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
} 