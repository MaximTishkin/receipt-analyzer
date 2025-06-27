package com.receiptanalyzer.service;

import com.receiptanalyzer.exception.RepositoryException;
import com.receiptanalyzer.exception.ServiceException;
import com.receiptanalyzer.generator.ReceiptDataGenerator;
import com.receiptanalyzer.model.ReceiptData;
import com.receiptanalyzer.repository.ReceiptRepository;
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

/**
 * Сервис для работы с чеками.
 */
@Slf4j
@Service
public class ReceiptService {
    private static final DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
    private final ReceiptRepository receiptRepository;
    private final ReceiptDataGenerator receiptDataGenerator;

    @Autowired
    public ReceiptService(ReceiptRepository receiptRepository, ReceiptDataGenerator receiptDataGenerator) {
        this.receiptRepository = receiptRepository;
        this.receiptDataGenerator = receiptDataGenerator;
    }

    /**
     * Заполняет данные чека по QR + генерация покупок/категорий.
     *
     * @param file файл с QR кодом
     * @param clientId id клиента
     * @return данные по чеку
     */
    public ReceiptData getDataByQr(MultipartFile file, Long clientId) {
        String qrText = recognizeQrCode(file);
        ReceiptData data = parseFnQrString(qrText, clientId);
        receiptDataGenerator.fillReceiptData(data);
        return data;
    }

    /**
     * Сохраняет данные чека.
     *
     * @param data данные по чеку
     */
    public void saveReceiptData(ReceiptData data) {
        try {
            receiptRepository.saveReceiptData(data);
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    private String recognizeQrCode(MultipartFile file) {
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
            log.error(errorMessage + ": " + e.getMessage(), e);
            throw new ServiceException(errorMessage);
        }
    }

    private ReceiptData parseFnQrString(String qr, Long clientId) {
        ReceiptData data = new ReceiptData();
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
} 