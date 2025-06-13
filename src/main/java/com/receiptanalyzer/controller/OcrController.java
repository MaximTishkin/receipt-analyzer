package com.receiptanalyzer.controller;

import com.receiptanalyzer.model.ReceiptInfo;
import com.receiptanalyzer.model.ReceiptDebugInfo;
import com.receiptanalyzer.service.OCRService;
import com.receiptanalyzer.util.ImageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR API", description = "API для распознавания текста на изображениях")
public class OcrController {

    private static final Logger log = LoggerFactory.getLogger(OcrController.class);
    private final OCRService ocrService;

    /**
     * Распознать текст на изображении и извлечь данные чека.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Распознать текст на изображении и извлечь данные чека")
    public ReceiptInfo recognizeText(@RequestParam("file") MultipartFile file) {
        return handleOcrRequest(() -> ocrService.processReceipt(ImageUtils.multipartFileToBufferedImage(file)));
    }

    /**
     * Распознать текст на изображении и вернуть отладочную информацию.
     */
    @PostMapping(path = "/debug", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Распознать текст на изображении и вернуть отладочную информацию")
    public ReceiptDebugInfo recognizeTextWithDebug(@RequestParam("file") MultipartFile file) {
        return handleOcrRequest(() -> ocrService.processReceiptWithDebug(ImageUtils.multipartFileToBufferedImage(file)));
    }

    /**
     * Унифицированная обработка ошибок для OCR endpoint'ов.
     */
    private <T> T handleOcrRequest(OcrSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Ошибка при распознавании текста", e);
            throw new RuntimeException("Ошибка при обработке изображения: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface OcrSupplier<T> {
        T get() throws Exception;
    }
} 