package com.receiptanalyzer.service;

import com.receiptanalyzer.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.awt.image.BufferedImage;
import java.util.Arrays;

@Slf4j
@Service
public class OCRService {
    private static final String allowedChars = "0123456789АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя.,()-:;/%₽ ";
    private static final Logger log = LoggerFactory.getLogger(OCRService.class);
    private final ImagePreprocessor imagePreprocessor;
    private Tesseract tesseract;
    
    @Value("${tesseract.data.path}")
    private String tesseractDataPath;
    
    @Value("${tesseract.language}")
    private String language;
    
    @Value("${tesseract.page.segmentation.mode:6}")
    private Integer pageSegMode;
    
    @Value("${tesseract.ocr.engine.mode:3}")
    private Integer ocrEngineMode;
    
    @Value("${receipt.recognition.confidence.threshold:60}")
    private Integer confidenceThreshold;

    @Autowired
    public OCRService(ImagePreprocessor imagePreprocessor) {
        this.imagePreprocessor = imagePreprocessor;
    }

    @PostConstruct
    private void initializeTesseract() {
        this.tesseract = new Tesseract();
        
        try {
            // Устанавливаем путь к данным Tesseract
            tesseract.setDatapath(tesseractDataPath);
            
            // Устанавливаем языки распознавания
            tesseract.setLanguage(language);
            
            // Настраиваем режим OCR движка
            tesseract.setOcrEngineMode(ocrEngineMode);
            
            // Устанавливаем режим сегментации страницы
            tesseract.setPageSegMode(pageSegMode);

            
            // Настраиваем параметры для лучшего распознавания чеков
            tesseract.setTessVariable("tessedit_char_whitelist", allowedChars);
            tesseract.setTessVariable("preserve_interword_spaces", "1");
            tesseract.setTessVariable("textord_heavy_nr", "1");
            tesseract.setTessVariable("tessedit_write_images", "1");
            
            // Устанавливаем дополнительные параметры для работы с русским языком
            tesseract.setTessVariable("user_defined_dpi", "300");
            tesseract.setTessVariable("debug_file", "/dev/null");
            tesseract.setTessVariable("tessedit_create_txt", "1");
            tesseract.setTessVariable("tessedit_create_hocr", "0");
            tesseract.setTessVariable("tessedit_pageseg_mode", pageSegMode.toString());
            tesseract.setTessVariable("tessedit_ocr_engine_mode", ocrEngineMode.toString());
            
            log.info("Tesseract initialized successfully with languages: {}", language);
        } catch (Exception e) {
            log.error("Failed to initialize Tesseract: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize OCR engine", e);
        }
    }

    public String processReceipt(BufferedImage image) throws TesseractException {
        try {
            log.info("Starting receipt processing");
            
            // Предварительная обработка изображения
            BufferedImage preprocessedImage = imagePreprocessor.preprocessImage(image);
            log.debug("Image preprocessing completed");

            // Сохраняем обработанное изображение
            ImageUtils.saveImageToFile(preprocessedImage, "preprocessed");
            
            // Выполняем OCR
            String result = tesseract.doOCR(preprocessedImage);
            log.debug("OCR processing completed. Raw result: {}", result);
            
            // Пост-обработка результата
            String processedResult = postProcessText(result);
            log.info("Receipt processing completed successfully. Processed result: {}", processedResult);
            
            return processedResult;
        } catch (TesseractException e) {
            log.error("Error during OCR processing: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during receipt processing: {}", e.getMessage());
            throw new RuntimeException("Failed to process receipt", e);
        }
    }

    private String postProcessText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        try {
            return Arrays.stream(text.split("\n"))
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .map(this::cleanupLine)
                        //.filter(this::isValidReceiptLine)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");
        } catch (Exception e) {
            log.error("Error during text post-processing: {}", e.getMessage());
            return text;
        }
    }

    private String cleanupLine(String line) {
        try {
            // Удаляем множественные пробелы
            line = line.replaceAll("\\s+", " ");
            
            // Исправляем часто встречающиеся ошибки распознавания
            //line = fixCommonOCRErrors(line);
            
            return line.trim();
        } catch (Exception e) {
            log.error("Error during line cleanup: {}", e.getMessage());
            return line;
        }
    }
    
    private String fixCommonOCRErrors(String line) {
        try {
            // Исправляем часто встречающиеся ошибки в суммах
            line = line.replaceAll("(?<=\\d)О(?=\\d)", "0"); // Замена буквы О на цифру 0
            line = line.replaceAll("(?<=\\d)l(?=\\d)", "1"); // Замена l на 1
            line = line.replaceAll("(?<=\\d)З(?=\\d)", "3"); // Замена З на 3
            
            // Исправляем пробелы в суммах
            line = line.replaceAll("(\\d+)\\s+(\\d{2})(?=\\s|$)", "$1.$2"); // 123 45 -> 123.45
            
            return line;
        } catch (Exception e) {
            log.error("Error during OCR error fixing: {}", e.getMessage());
            return line;
        }
    }
    
    private boolean isValidReceiptLine(String line) {
        try {
            // Проверяем, содержит ли строка полезную информацию
            if (line.length() < 3) return false;
            
            // Проверяем, содержит ли строка хотя бы одну цифру
            // но не требуем, чтобы вся строка состояла только из цифр
            if (!line.matches(".*\\d+.*")) return false;
            
            return true;
        } catch (Exception e) {
            log.error("Error during line validation: {}", e.getMessage());
            return false;
        }
    }
} 