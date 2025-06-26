package com.receiptanalyzer.service;

import lombok.extern.slf4j.Slf4j;
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
public class OCRService {

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
     * Данные, полученные из QR-кода ФНС.
     */
    public static class FnQrData {
        public String dateTime;
        public String formattedDateTime;
        public String sum;
        public String fn;
        public String fiscalNumber;
        public String fiscalSign;
        public String docType;
    }

    /**
     * Парсит строку из QR-кода ФНС и возвращает распознанные данные.
     */
    public FnQrData parseFnQrString(String qr) {
        FnQrData data = new FnQrData();
        if (qr == null) return data;
        String[] parts = qr.split("&");
        for (String part : parts) {
            if (part.startsWith("t=")) data.dateTime = part.substring(2);
            else if (part.startsWith("s=")) data.sum = part.substring(2);
            else if (part.startsWith("fn=")) data.fn = part.substring(3);
            else if (part.startsWith("i=")) data.fiscalNumber = part.substring(2);
            else if (part.startsWith("fp=")) data.fiscalSign = part.substring(3);
            else if (part.startsWith("n=")) data.docType = part.substring(2);
        }
        // Форматируем дату и время
        if (data.dateTime != null) {
            try {
                DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
                DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                LocalDateTime dt = LocalDateTime.parse(data.dateTime, inputFmt);
                data.formattedDateTime = dt.format(outputFmt);
            } catch (DateTimeParseException e) {
                data.formattedDateTime = data.dateTime;
            }
        }
        return data;
    }
} 