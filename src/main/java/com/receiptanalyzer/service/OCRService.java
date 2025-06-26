package com.receiptanalyzer.service;

import com.receiptanalyzer.model.ReceiptInfo;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

    /**
     * Распознаёт QR-код на изображении и возвращает его содержимое.
     */
    public String recognizeQrCode(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            log.warn("QR-код не найден на изображении");
            return null;
        } catch (Exception e) {
            log.error("Ошибка при распознавании QR-кода: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Данные, полученные из QR-кода ФНС.
     */
    public static class FnQrData {
        public String dateTime;
        public String formattedDateTime;
        public String sum;
        public String fn;
        public String fiscalNumber;
        public String fiscalSign;
        public String docType;
    }

    /**
     * Парсит строку из QR-кода ФНС и возвращает распознанные данные.
     */
    public FnQrData parseFnQrString(String qr) {
        FnQrData data = new FnQrData();
        if (qr == null) return data;
        String[] parts = qr.split("&");
        for (String part : parts) {
            if (part.startsWith("t=")) data.dateTime = part.substring(2);
            else if (part.startsWith("s=")) data.sum = part.substring(2);
            else if (part.startsWith("fn=")) data.fn = part.substring(3);
            else if (part.startsWith("i=")) data.fiscalNumber = part.substring(2);
            else if (part.startsWith("fp=")) data.fiscalSign = part.substring(3);
            else if (part.startsWith("n=")) data.docType = part.substring(2);
        }
        // Форматируем дату и время
        if (data.dateTime != null) {
            try {
                DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
                DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                LocalDateTime dt = LocalDateTime.parse(data.dateTime, inputFmt);
                data.formattedDateTime = dt.format(outputFmt);
            } catch (DateTimeParseException e) {
                data.formattedDateTime = data.dateTime;
            }
        }
        return data;
    }
} 