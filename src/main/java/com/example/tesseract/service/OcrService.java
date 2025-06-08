package com.example.tesseract.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

@Slf4j
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);
    private final Tesseract tesseract;

    public OcrService(
            @Value("${tesseract.data.path}") String dataPath,
            @Value("${tesseract.languages}") String languages
    ) {
        tesseract = new Tesseract();
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(languages);

        log.info("Инициализация Tesseract OCR");
        log.info("Путь к данным: {}", dataPath);
        log.info("Языки: {}", languages);
    }

    public String performOcr(MultipartFile file) throws Exception {
        try {
            // Проверка формата файла
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".jpg") && 
                !filename.toLowerCase().endsWith(".jpeg") && 
                !filename.toLowerCase().endsWith(".png"))) {
                throw new IllegalArgumentException("Поддерживаются только файлы JPG и PNG");
            }

            // Конвертация MultipartFile в BufferedImage
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("Не удалось прочитать изображение");
            }

            // Распознавание текста
            return tesseract.doOCR(image);
        } catch (Exception e) {
            log.error("Ошибка при распознавании текста", e);
            throw e;
        }
    }
} 