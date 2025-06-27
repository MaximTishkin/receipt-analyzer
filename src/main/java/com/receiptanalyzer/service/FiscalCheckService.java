package com.receiptanalyzer.service;

import com.receiptanalyzer.exception.RepositoryException;
import com.receiptanalyzer.exception.ServiceException;
import com.receiptanalyzer.model.FiscalCheckData;
import com.receiptanalyzer.repository.FiscalCheckRepository;
import com.receiptanalyzer.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.web.multipart.MultipartFile;

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
    public String recognizeQrCode(MultipartFile file) {
        try {
            BufferedImage image = ImageUtils.multipartFileToBufferedImage(file);
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            String errorMessage = "QR-код не найден на изображении";
            log.warn(errorMessage);
            throw new ServiceException(errorMessage);
        } catch (Exception e) {
            String errorMessage = "Ошибка при распознавании QR-кода";
            log.error(errorMessage + e.getMessage(), e);
            throw new ServiceException(errorMessage);
        }
    }

    /**
     * Парсит строку из QR-кода ФНС и возвращает распознанные данные.
     */
    public FiscalCheckData parseFnQrString(String qr, Long clientId) {
        FiscalCheckData data = new FiscalCheckData();
        data.setClientId(clientId);
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
                    log.error("Не известный формат номера чека");
                }
            }
        }
        // Номер смены поке генерится рандомный, этой информации нет в QR
        data.setShiftNumber(ThreadLocalRandom.current().nextLong(1L, 201L));
        return data;
    }

    public void saveCheckData(FiscalCheckData data) {
        try {
            fiscalCheckRepository.saveCheckData(data);
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }

    }
} 