package com.example.tesseract.controller;

import com.example.tesseract.service.OcrService;
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
    private final OcrService ocrService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Распознать текст на изображении")
    public String recognizeText(@RequestParam("file") MultipartFile file) {
        try {
            return ocrService.performOcr(file);
        } catch (Exception e) {
            log.error("Ошибка при распознавании текста", e);
            throw new RuntimeException("Ошибка при обработке изображения: " + e.getMessage());
        }
    }
} 