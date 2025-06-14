package com.receiptanalyzer.service;

import com.receiptanalyzer.model.ReceiptInfo;
import com.receiptanalyzer.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class AsyncOcrService {
    private final OCRService ocrService;

    @Autowired
    public AsyncOcrService(OCRService ocrService) {
        this.ocrService = ocrService;
    }

    @Async
    public void processAsync(MultipartFile file) {
        try {
            ReceiptInfo info = ocrService.processReceipt(ImageUtils.multipartFileToBufferedImage(file));
            log.info("[ASYNC OCR] Дата: {}, Сумма: {}, Категория: {}", info.getDate(), info.getTotalAmount(), info.getCategory() != null ? info.getCategory().getDisplayName() : null);
        } catch (Exception e) {
            log.error("[ASYNC OCR] Ошибка при обработке файла: {}", e.getMessage(), e);
        }
    }
} 