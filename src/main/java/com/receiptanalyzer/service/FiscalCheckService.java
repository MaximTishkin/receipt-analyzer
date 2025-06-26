package com.receiptanalyzer.service;

import com.receiptanalyzer.model.FnQrData;
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

@Slf4j
@Service
public class FiscalCheckService {
    private FiscalCheckRepository fiscalCheckRepository;

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
    public FnQrData parseFnQrString(String qr) {
        FnQrData data = new FnQrData();
        if (qr == null) return data;
        String[] parts = qr.split("&");
        for (String part : parts) {
            if (part.startsWith("t=")) data.setDateTime(part.substring(2));
            else if (part.startsWith("s=")) data.setSum(part.substring(2));
            else if (part.startsWith("fn=")) data.setFn(part.substring(3));
            else if (part.startsWith("i=")) data.setFiscalNumber(part.substring(2));
            else if (part.startsWith("fp=")) data.setFiscalSign(part.substring(3));
            else if (part.startsWith("n=")) data.setDocType(part.substring(2));
        }
        // Форматируем дату и время
        if (data.getDateTime() != null) {
            try {
                DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
                DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                LocalDateTime dt = LocalDateTime.parse(data.getDateTime(), inputFmt);
                data.setFormattedDateTime(dt.format(outputFmt));
            } catch (DateTimeParseException e) {
                data.setFormattedDateTime(data.getDateTime());
            }
        }
        fiscalCheckRepository.saveCheckData(data);
        return data;
    }
} 