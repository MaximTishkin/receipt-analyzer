package com.receiptanalyzer.controller;

import com.receiptanalyzer.service.OCRService;
import com.receiptanalyzer.util.ImageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    /**
     * Распознать QR-код на изображении и вернуть распознанные данные.
     */
    @PostMapping(path = "/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Распознать QR-код на изображении и вернуть распознанные данные")
    public ResponseEntity<OCRService.FnQrData> recognizeQr(@RequestParam("file") MultipartFile file) {
        try {
            String qrText = ocrService.recognizeQrCode(ImageUtils.multipartFileToBufferedImage(file));
            if (qrText == null) {
                return ResponseEntity.badRequest().body(null);
            }
            OCRService.FnQrData data = ocrService.parseFnQrString(qrText);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Ошибка при распознавании QR-кода", e);
            return ResponseEntity.badRequest().body(null);
        }
    }
} 