package com.receiptanalyzer.service;

import com.receiptanalyzer.model.ReceiptInfo;
import com.receiptanalyzer.model.ReceiptDebugInfo;
import com.receiptanalyzer.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
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
    private static final String ALLOWED_CHARS = "0123456789АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя.,()-:;/%₽ ";
    private static final Logger log = LoggerFactory.getLogger(OCRService.class);
    private final ImagePreprocessor imagePreprocessor;
    private final ReceiptDataExtractor dataExtractor;
    private final TextCorrectionService textCorrectionService;
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
    public OCRService(ImagePreprocessor imagePreprocessor, 
                     ReceiptDataExtractor dataExtractor,
                     TextCorrectionService textCorrectionService) {
        this.imagePreprocessor = imagePreprocessor;
        this.dataExtractor = dataExtractor;
        this.textCorrectionService = textCorrectionService;
    }

    /**
     * Инициализация Tesseract с нужными параметрами.
     */
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
            tesseract.setTessVariable("tessedit_char_whitelist", ALLOWED_CHARS);
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

    /**
     * Обрабатывает изображение чека с возвратом отладочной информации.
     */
    public ReceiptDebugInfo processReceiptWithDebug(BufferedImage image) {
        try {
            log.info("Starting receipt processing with debug info");
            
            // Предварительная обработка изображения
            BufferedImage preprocessedImage = preprocessImage(image);
            log.debug("Image preprocessing completed");

            // Сохраняем обработанное изображение
            ImageUtils.saveImageToFile(preprocessedImage, "preprocessed");
            
            // Выполняем OCR
            String rawText = doOcr(preprocessedImage);
            log.debug("OCR processing completed. Raw result: {}", rawText);
            
            // Очищаем текст
            String cleanedText = postProcessText(rawText);
            log.debug("Text cleaned. Result: {}", cleanedText);
            
            // Извлекаем структурированные данные
            ReceiptInfo receiptInfo = dataExtractor.extractData(cleanedText);
            log.info("Receipt data extracted: {}", receiptInfo);
            
            return ReceiptDebugInfo.builder()
                    .receiptInfo(receiptInfo)
                    .rawText(rawText)
                    .cleanedText(cleanedText)
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error during receipt processing: {}", e.getMessage());
            throw new RuntimeException("Failed to process receipt", e);
        }
    }

    /**
     * Обрабатывает изображение чека и возвращает только структурированные данные.
     */
    public ReceiptInfo processReceipt(BufferedImage image) {
        return processReceiptWithDebug(image).getReceiptInfo();
    }

    /**
     * Предварительная обработка изображения.
     */
    private BufferedImage preprocessImage(BufferedImage image) {
        return imagePreprocessor.preprocessImage(image);
    }

    /**
     * Выполняет OCR над изображением.
     */
    private String doOcr(BufferedImage image) throws Exception {
        return tesseract.doOCR(image);
    }

    /**
     * Постобработка текста после OCR: исправление ошибок, очистка и фильтрация строк.
     */
    private String postProcessText(String text) {
        if (isNullOrEmpty(text)) return "";
        try {
            // Сначала исправляем ошибки в тексте
            String correctedText = textCorrectionService.correctText(text);
            
            // Затем применяем стандартную обработку
            return Arrays.stream(correctedText.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(this::cleanupLine)
                    .filter(this::isValidReceiptLine)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        } catch (Exception e) {
            log.error("Error during text post-processing: {}", e.getMessage());
            return text;
        }
    }

    /**
     * Удаляет множественные пробелы в строке.
     */
    private String cleanupLine(String line) {
        try {
            // Удаляем множественные пробелы
            line = line.replaceAll("\\s+", " ");
            
            return line.trim();
        } catch (Exception e) {
            log.error("Error during line cleanup: {}", e.getMessage());
            return line;
        }
    }
    
    /**
     * Проверяет, содержит ли строка полезную информацию.
     */
    private boolean isValidReceiptLine(String line) {
        try {
            // Проверяем, содержит ли строка полезную информацию
            return line.length() >= 3;
        } catch (Exception e) {
            log.error("Error during line validation: {}", e.getMessage());
            return false;
        }
    }

    private boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
} 