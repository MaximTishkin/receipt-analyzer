package com.receiptanalyzer.service;

import org.languagetool.JLanguageTool;
import org.languagetool.language.Russian;
import org.languagetool.rules.RuleMatch;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
public class TextCorrectionService {
    
    private JLanguageTool langTool;
    
    @PostConstruct
    public void init() {
        try {
            // Инициализируем LanguageTool с русским языком
            langTool = new JLanguageTool(new Russian());
            
            // Отключаем правила, которые могут мешать при обработке чеков
            langTool.disableRule("UPPERCASE_SENTENCE_START"); // Не требуем заглавную букву в начале
            langTool.disableRule("RU_COMPOUNDS"); // Отключаем проверку составных слов
            
            // Включаем более агрессивную проверку орфографии
            langTool.enableRule("MORFOLOGIK_RULE_RU_RU");
            langTool.enableRule("SPELLING_RULE");
            
            log.info("TextCorrectionService initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize TextCorrectionService: {}", e.getMessage());
        }
    }
    
    public String correctText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // Разбиваем текст на строки для обработки
            String[] lines = text.split("\n");
            StringBuilder correctedText = new StringBuilder();
            
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    correctedText.append("\n");
                    continue;
                }
                
                String correctedLine = correctLine(line);
                correctedText.append(correctedLine).append("\n");
            }
            
            return correctedText.toString().trim();
        } catch (Exception e) {
            log.error("Error during text correction: {}", e.getMessage());
            return text;
        }
    }
    
    private String correctLine(String line) {
        try {
            // Пропускаем строки, содержащие только цифры или специальные символы
            if (line.matches("^[\\d\\s.,₽-]+$")) {
                return line;
            }
            
            // Получаем предложения для исправления от LanguageTool
            List<RuleMatch> matches = langTool.check(line);
            
            // Если нет ошибок, возвращаем исходную строку
            if (matches.isEmpty()) {
                return line;
            }
            
            // Применяем исправления с конца строки, чтобы не сбить индексы
            StringBuilder corrected = new StringBuilder(line);
            for (int i = matches.size() - 1; i >= 0; i--) {
                RuleMatch match = matches.get(i);
                
                // Получаем предложенные исправления
                List<String> suggestions = match.getSuggestedReplacements();
                if (!suggestions.isEmpty()) {
                    // Берем первое предложенное исправление
                    String replacement = suggestions.get(0);
                    
                    // Сохраняем регистр исходного текста
                    String original = line.substring(match.getFromPos(), match.getToPos());
                    if (isUpperCase(original)) {
                        replacement = replacement.toUpperCase();
                    }
                    
                    // Применяем исправление
                    corrected.replace(match.getFromPos(), match.getToPos(), replacement);
                }
            }
            
            return corrected.toString();
        } catch (Exception e) {
            log.error("Error during line correction: {}", e.getMessage());
            return line;
        }
    }
    
    private boolean isUpperCase(String text) {
        return text.equals(text.toUpperCase());
    }
} 