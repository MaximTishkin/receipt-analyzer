package com.receiptanalyzer.service;

import org.languagetool.JLanguageTool;
import org.languagetool.language.Russian;
import org.languagetool.rules.RuleMatch;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TextCorrectionService {
    private JLanguageTool langTool;

    /**
     * Инициализация LanguageTool для русского языка с нужными правилами.
     */
    @PostConstruct
    public void init() {
        try {
            langTool = new JLanguageTool(new Russian());
            langTool.disableRule("UPPERCASE_SENTENCE_START");
            langTool.disableRule("RU_COMPOUNDS");
            langTool.enableRule("MORFOLOGIK_RULE_RU_RU");
            langTool.enableRule("SPELLING_RULE");
            log.info("TextCorrectionService initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize TextCorrectionService: {}", e.getMessage());
        }
    }

    /**
     * Исправляет орфографические ошибки в тексте с помощью LanguageTool.
     * Сохраняет структуру и регистр исходного текста.
     * @param text исходный текст
     * @return исправленный текст
     */
    public String correctText(String text) {
        if (isNullOrEmpty(text)) return text;
        return java.util.Arrays.stream(text.split("\n"))
                .map(this::correctLineSafe)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Безопасная обертка для исправления одной строки (не выбрасывает исключения).
     */
    private String correctLineSafe(String line) {
        try {
            return correctLine(line);
        } catch (Exception e) {
            log.error("Error during line correction: {}", e.getMessage());
            return line;
        }
    }

    /**
     * Исправляет орфографические ошибки в одной строке.
     * Пропускает строки, содержащие только цифры или спецсимволы.
     */
    private String correctLine(String line) throws Exception {
        if (isNullOrEmpty(line) || isNumericOrSpecial(line)) {
            return line;
        }
        List<RuleMatch> matches = langTool.check(line);
        if (matches.isEmpty()) return line;
        StringBuilder corrected = new StringBuilder(line);
        for (int i = matches.size() - 1; i >= 0; i--) {
            RuleMatch match = matches.get(i);
            List<String> suggestions = match.getSuggestedReplacements();
            if (!suggestions.isEmpty()) {
                String replacement = preserveCase(suggestions.get(0), line.substring(match.getFromPos(), match.getToPos()));
                corrected.replace(match.getFromPos(), match.getToPos(), replacement);
            }
        }
        return corrected.toString();
    }

    /**
     * Проверяет, является ли строка пустой или null.
     */
    private boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Проверяет, состоит ли строка только из цифр, пробелов и спецсимволов.
     */
    private boolean isNumericOrSpecial(String text) {
        return text.matches("^[\\d\\s.,₽-]+$");
    }

    /**
     * Сохраняет регистр исходного слова при замене.
     */
    private String preserveCase(String replacement, String original) {
        if (original.equals(original.toUpperCase())) {
            return replacement.toUpperCase();
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        return replacement;
    }
} 