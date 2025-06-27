package com.receiptanalyzer.service;

import com.receiptanalyzer.exception.RepositoryException;
import com.receiptanalyzer.model.FiscalCheckData;
import com.receiptanalyzer.repository.FiscalCheckRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class FiscalCheckService {
    private static final DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
    private final FiscalCheckRepository fiscalCheckRepository;

    @Autowired
    public FiscalCheckService(FiscalCheckRepository fiscalCheckRepository) {
        this.fiscalCheckRepository = fiscalCheckRepository;
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
     * Парсит строку из QR-кода ФНС и возвращает распознанные данные.
     */
    public FiscalCheckData parseFnQrString(String qr) throws RepositoryException {
        FiscalCheckData data = new FiscalCheckData();
        if (qr != null) {
            String[] parts = qr.split("&");
            for (String part : parts) {
                if (part.startsWith("t=")) {
                    LocalDateTime date = LocalDateTime.now();
                    try {
                        date = LocalDateTime.parse(part.substring(2), inputFmt);
                    } catch (DateTimeParseException e) {
                        log.error("Не известный формат даты");
                    }
                    data.setCheckDate(date);
                } else if (part.startsWith("s=")){
                    try {
                        data.setAmount(Double.parseDouble(part.substring(2)));
                    } catch (NumberFormatException e) {
                        log.error("Не известный формат суммы");
                    }
                } else if (part.startsWith("fn=")) {
                    data.setDeviceRegNumber(part.substring(3));
                } else if (part.startsWith("i=")) {
                    try {
                        data.setCheckNumberInShift(Long.parseLong(part.substring(2)));
                    } catch (NumberFormatException e) {
                        log.error("Не известный формат омера чека");
                    }
                }
            }
            // Номер смены поке генерится рандомный, этой информации нет в QR
            data.setShiftNumber(ThreadLocalRandom.current().nextLong(1L, 201L));
            fiscalCheckRepository.saveCheckData(data);
        }
        return data;
    }
} 