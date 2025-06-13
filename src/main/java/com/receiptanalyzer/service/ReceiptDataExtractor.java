package com.receiptanalyzer.service;

import com.receiptanalyzer.model.ReceiptInfo;
import com.receiptanalyzer.model.ReceiptInfo.Category;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ReceiptDataExtractor {

    // Паттерны для поиска даты
    private static final List<DatePattern> DATE_PATTERNS = Arrays.asList(
        new DatePattern("dd.MM.yyyy", "\\b\\d{2}\\.\\d{2}\\.\\d{4}\\b"),
        new DatePattern("dd.MM.yy", "\\b\\d{2}\\.\\d{2}\\.\\d{2}\\b"),
        new DatePattern("yyyy-MM-dd", "\\b\\d{4}-\\d{2}-\\d{2}\\b"),
        new DatePattern("dd/MM/yyyy", "\\b\\d{2}/\\d{2}/\\d{4}\\b")
    );

    // Паттерн для поиска суммы после найденного ключевого слова
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "\\s*[:=]?\\s*(\\d+[.,]\\d{2})\\D*(?:руб(?:лей)?|₽)?",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    // Минимальная и максимальная сумма для валидации
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");

    // Ключевые слова для поиска суммы
    private static final Set<String> AMOUNT_KEYWORDS = new HashSet<>(Arrays.asList(
        "итого", "итог", "всего", "к оплате", "сумма", "наличными", "оплата"
    ));

    // Максимальное расстояние Левенштейна для нечеткого поиска
    private static final int MAX_LEVENSHTEIN_DISTANCE = 2;

    // Словари для категорий
    private static final Map<Category, Set<String>> CATEGORY_KEYWORDS = new EnumMap<>(Category.class);
    
    static {
        // Продукты
        CATEGORY_KEYWORDS.put(Category.GROCERIES, new HashSet<>(Arrays.asList(
            "молоко", "хлеб", "сыр", "колбаса", "йогурт", "творог", "масло",
            "овощи", "фрукты", "мясо", "рыба", "яйца", "сахар", "соль",
            "крупа", "макароны", "чай", "кофе", "сок", "вода", "печенье",
            "конфеты", "шоколад", "продукты", "супермаркет", "гастроном", "ряженка"
        )));

        // Одежда
        CATEGORY_KEYWORDS.put(Category.CLOTHING, new HashSet<>(Arrays.asList(
            "футболка", "брюки", "джинсы", "куртка", "пальто", "платье",
            "юбка", "рубашка", "носки", "обувь", "ботинки", "кроссовки",
            "одежда", "бутик", "магазин одежды", "шарф", "перчатки", "белье",
            "свитер", "пиджак", "костюм", "кеды"
        )));

        // Стройматериалы
        CATEGORY_KEYWORDS.put(Category.CONSTRUCTION, new HashSet<>(Arrays.asList(
            "краска", "обои", "цемент", "песок", "гипс", "шпаклевка",
            "грунтовка", "плитка", "ламинат", "паркет", "гвозди", "шурупы",
            "дрель", "молоток", "пила", "строительный", "стройматериалы",
            "инструмент", "саморезы", "герметик", "клей", "лак"
        )));
    }

    public ReceiptInfo extractData(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        return ReceiptInfo.builder()
                .date(extractDate(text))
                .totalAmount(extractTotalAmount(text))
                .category(determineCategory(text))
                .build();
    }

    private LocalDate extractDate(String text) {
        for (DatePattern pattern : DATE_PATTERNS) {
            try {
                Matcher matcher = Pattern.compile(pattern.regex()).matcher(text);
                if (matcher.find()) {
                    String dateStr = matcher.group().replace(",", ".");
                    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern.format()));
                }
            } catch (DateTimeParseException e) {
                log.debug("Failed to parse date with pattern {}: {}", pattern.format(), e.getMessage());
            }
        }
        return null;
    }

    private BigDecimal extractTotalAmount(String text) {
        try {
            // Разбиваем текст на строки для поиска
            String[] lines = text.toLowerCase().split("\n");
            
            BigDecimal maxAmount = null;
            
            // Сначала ищем суммы после ключевых слов
            for (String line : lines) {
                String foundKeyword = findClosestKeyword(line);
                if (foundKeyword != null) {
                    int keywordIndex = line.indexOf(foundKeyword);
                    String afterKeyword = line.substring(keywordIndex + foundKeyword.length());
                    
                    Matcher matcher = AMOUNT_PATTERN.matcher(afterKeyword);
                    while (matcher.find()) {
                        String amountStr = matcher.group(1).replace(",", ".");
                        try {
                            BigDecimal amount = new BigDecimal(amountStr);
                            if (isValidAmount(amount)) {
                                if (maxAmount == null || amount.compareTo(maxAmount) > 0) {
                                    maxAmount = amount;
                                    log.debug("Found new max amount {} after keyword {} in line: {}", 
                                            amount, foundKeyword, line);
                                }
                            }
                        } catch (NumberFormatException e) {
                            log.debug("Failed to parse amount: {}", amountStr);
                        }
                    }
                }
            }
            
            // Если не нашли сумму после ключевых слов, ищем любую подходящую сумму
            if (maxAmount == null) {
                Pattern anyAmount = Pattern.compile("(\\d+[.,]\\d{2})\\s*(?:руб(?:лей)?|₽)?");
                for (String line : lines) {
                    // Пропускаем строки с НДС и скидками
                    if (line.contains("ндс") || line.contains("скидк")) {
                        continue;
                    }
                    
                    Matcher matcher = anyAmount.matcher(line);
                    while (matcher.find()) {
                        String amountStr = matcher.group(1).replace(",", ".");
                        try {
                            BigDecimal amount = new BigDecimal(amountStr);
                            if (isValidAmount(amount)) {
                                if (maxAmount == null || amount.compareTo(maxAmount) > 0) {
                                    maxAmount = amount;
                                    log.debug("Found new max amount {} in line: {}", amount, line);
                                }
                            }
                        } catch (NumberFormatException e) {
                            log.debug("Failed to parse amount: {}", amountStr);
                        }
                    }
                }
            }
            
            return maxAmount;

        } catch (Exception e) {
            log.error("Failed to parse total amount: {}", e.getMessage());
            return null;
        }
    }

    private boolean isValidAmount(BigDecimal amount) {
        return amount != null && 
               amount.compareTo(MIN_AMOUNT) >= 0 && 
               amount.compareTo(MAX_AMOUNT) <= 0;
    }

    private String findClosestKeyword(String line) {
        String bestMatch = null;
        int minDistance = MAX_LEVENSHTEIN_DISTANCE + 1;

        // Разбиваем строку на слова
        String[] words = line.split("\\s+");

        for (String word : words) {
            for (String keyword : AMOUNT_KEYWORDS) {
                // Для составных ключевых слов (например, "к оплате")
                String[] keywordParts = keyword.split("\\s+");
                if (keywordParts.length > 1) {
                    // Проверяем, есть ли достаточно слов для сравнения
                    int wordIndex = Arrays.asList(words).indexOf(word);
                    if (wordIndex + keywordParts.length <= words.length) {
                        // Собираем фразу той же длины, что и ключевое слово
                        String phrase = String.join(" ", Arrays.copyOfRange(words, wordIndex, wordIndex + keywordParts.length));
                        int distance = levenshteinDistance(phrase, keyword);
                        if (distance < minDistance) {
                            minDistance = distance;
                            bestMatch = keyword;
                        }
                    }
                } else {
                    // Для одиночных слов
                    int distance = levenshteinDistance(word, keyword);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestMatch = keyword;
                    }
                }
            }
        }

        return bestMatch;
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        int[] curr = new int[s2.length() + 1];

        for (int j = 0; j <= s2.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(
                    curr[j - 1] + 1,     // insertion
                    prev[j] + 1),        // deletion
                    prev[j - 1] + cost); // substitution
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[s2.length()];
    }

    private Category determineCategory(String text) {
        // Создаем карту для подсчета совпадений по каждой категории
        Map<Category, Integer> matchCounts = new EnumMap<>(Category.class);
        
        for (Map.Entry<Category, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            Category category = entry.getKey();
            Set<String> keywords = entry.getValue();
            
            int matches = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    matches++;
                }
            }
            
            if (matches > 0) {
                matchCounts.put(category, matches);
            }
        }
        
        if (matchCounts.isEmpty()) {
            return null;
        }
        
        // Возвращаем категорию с наибольшим количеством совпадений
        return Collections.max(matchCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private record DatePattern(String format, String regex) {}
} 