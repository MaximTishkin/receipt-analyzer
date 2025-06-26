package com.receiptanalyzer.controller;

import com.receiptanalyzer.model.FnQrData;
import com.receiptanalyzer.service.FiscalCheckService;
import com.receiptanalyzer.util.ImageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Tag(name = "OCR API", description = "API для распознавания текста на изображениях")
public class FiscalCheckController {
    private final FiscalCheckService fiscalCheckService;

    @Autowired
    public FiscalCheckController(FiscalCheckService fiscalCheckService) {
        this.fiscalCheckService = fiscalCheckService;
    }

    /**
     * Распознать QR-код на изображении и вернуть распознанные данные.
     */
    @PostMapping(path = "/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Распознать QR-код на изображении и вернуть распознанные данные")
    public ResponseEntity<FnQrData> recognizeQr(@RequestParam("file") MultipartFile file) {
        try {
            String qrText = fiscalCheckService.recognizeQrCode(ImageUtils.multipartFileToBufferedImage(file));
            if (qrText == null) {
                return ResponseEntity.badRequest().body(null);
            }
            FnQrData data = fiscalCheckService.parseFnQrString(qrText);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Ошибка при распознавании QR-кода", e);
            return ResponseEntity.badRequest().body(null);
        }
    }
} 