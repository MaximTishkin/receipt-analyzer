package com.receiptanalyzer.service;

import org.languagetool.JLanguageTool;
import org.languagetool.language.Russian;
import org.languagetool.rules.RuleMatch;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TextCorrectionService {

    private JLanguageTool langTool;
    
    @Value("${text.correction.min.word.length:3}")
    private int minWordLength;
    
    @Value("${text.correction.max.suggestions:1}")
    private int maxSuggestions;

    @PostConstruct
    private void init() {
        // Инициализируем LanguageTool с русским языком
        langTool = new JLanguageTool(new Russian());
        
        // Отключаем некоторые правила, которые могут быть излишними для чеков
        langTool.disableRule("UPPERCASE_SENTENCE_START"); // Начало предложения с большой буквы
        langTool.disableRule("COMMA_PARENTHESIS_WHITESPACE"); // Пробелы вокруг скобок
        langTool.disableRule("RU_COMPOUNDS"); // Правила дефисного написания
        langTool.disableRule("WORD_REPEAT_RULE"); // Повторы слов
        langTool.disableRule("UPPERCASE_SENTENCE_START"); // Заглавные буквы в начале предложения
        langTool.disableRule("PUNCTUATION_PARAGRAPH_END"); // Знаки препинания в конце параграфа
    }

    public String correctText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            // Разбиваем текст на строки и обрабатываем каждую отдельно
            return Arrays.stream(text.split("\n"))
                        .map(this::correctLine)
                        .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Error during text correction: {}", e.getMessage());
            return text;
        }
    }

    private String correctLine(String line) {
        try {
            StringBuilder corrected = new StringBuilder(line);
            List<RuleMatch> matches = langTool.check(line);

            // Применяем исправления с конца строки, чтобы не сбивать индексы
            for (int i = matches.size() - 1; i >= 0; i--) {
                RuleMatch match = matches.get(i);
                String word = line.substring(match.getFromPos(), match.getToPos());

                // Пропускаем слова короче минимальной длины
                if (word.length() < minWordLength) {
                    continue;
                }

                // Пропускаем слова, содержащие цифры (суммы, артикулы и т.д.)
                if (word.matches(".*\\d+.*")) {
                    continue;
                }

                // Получаем предложения по исправлению
                List<String> suggestions = match.getSuggestedReplacements();
                if (!suggestions.isEmpty()) {
                    // Берем только первые N предложений (обычно самые вероятные)
                    String replacement = suggestions.stream()
                            .limit(maxSuggestions)
                            .findFirst()
                            .orElse(word);
                    
                    // Проверяем, что замена не сильно отличается по длине
                    if (Math.abs(replacement.length() - word.length()) <= 2) {
                        corrected.replace(match.getFromPos(), match.getToPos(), replacement);
                        log.debug("Corrected '{}' to '{}' at position {}", 
                                word, replacement, match.getFromPos());
                    }
                }
            }

            return corrected.toString();
        } catch (Exception e) {
            log.error("Error correcting line '{}': {}", line, e.getMessage());
            return line;
        }
    }
} 