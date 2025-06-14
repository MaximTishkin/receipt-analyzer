package com.receiptanalyzer.config;

import net.sourceforge.tess4j.Tesseract;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Russian;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    private static final String ALLOWED_CHARS = "0123456789АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя.,()-:;/%₽ ";

    @Bean
    public Tesseract getTesseract(@Value("${tesseract.data.path}") String tesseractDataPath,
                                  @Value("${tesseract.language}") String language,
                                  @Value("${tesseract.page.segmentation.mode:6}") Integer pageSegMode,
                                  @Value("${tesseract.ocr.engine.mode:3}") Integer ocrEngineMode,
                                  @Value("${receipt.recognition.confidence.threshold:60}") Integer confidenceThreshold) {
        Tesseract tesseract = new Tesseract();
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
        return tesseract;
    }

    @Bean
    public JLanguageTool getJLanguageTool() {
        JLanguageTool langTool = new JLanguageTool(new Russian());
        langTool.disableRule("UPPERCASE_SENTENCE_START");
        langTool.disableRule("RU_COMPOUNDS");
        langTool.enableRule("MORFOLOGIK_RULE_RU_RU");
        langTool.enableRule("SPELLING_RULE");
        return langTool;
    }
}
