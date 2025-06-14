package com.receiptanalyzer.controller;

import com.receiptanalyzer.model.ReceiptInfo;
import com.receiptanalyzer.service.OCRService;
import com.receiptanalyzer.service.AsyncOcrService;
import com.receiptanalyzer.util.ImageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/receipt")
@RequiredArgsConstructor
@Tag(name = "OCR API", description = "API для распознавания текста на изображениях")
public class OcrController {
    private final OCRService ocrService;
    private final AsyncOcrService asyncOcrService;

    /**
     * Распознать текст на изображении и извлечь данные чека.
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Распознать текст на изображении и извлечь данные чека")
    public ReceiptInfo recognizeText(@RequestParam("file") MultipartFile file) {
        try {
            return ocrService.processReceipt(ImageUtils.multipartFileToBufferedImage(file));
        } catch (Exception e) {
            log.error("Ошибка при распознавании текста", e);
            throw new RuntimeException("Ошибка при обработке изображения: " + e.getMessage());
        }
    }

    /**
     * Асинхронная обработка файла: сразу возвращает 200 OK, результат логируется.
     */
    @PostMapping(path = "/upload/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Асинхронно распознать текст на изображении и залогировать результат")
    public ResponseEntity<String> recognizeTextAsync(@RequestParam("file") MultipartFile file) {
        asyncOcrService.processAsync(file);
        return ResponseEntity.ok("Файл принят в обработку");
    }

    /**
     * Глобальный обработчик ошибок для OCR endpoint'ов.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        log.error("Ошибка при обработке OCR запроса", e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }
} 