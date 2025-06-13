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

    // Паттерн для поиска суммы
    private static final Pattern TOTAL_AMOUNT_PATTERN = Pattern.compile(
        "(?i)(итого?|всего|к оплате|сумма|наличными)[\\s:]*?(\\d+[.,]\\d{2}|\\d+)\\D*(?:руб(?:лей)?|₽)?",
        Pattern.UNICODE_CHARACTER_CLASS
    );

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

        // Предварительная обработка текста
        text = text.toLowerCase()
                  .replace(",", ".")
                  .replaceAll("\\s+", " ")
                  .trim();

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
            Matcher matcher = TOTAL_AMOUNT_PATTERN.matcher(text);
            if (matcher.find()) {
                String amount = matcher.group(2).replace(",", ".");
                return new BigDecimal(amount);
            }
        } catch (NumberFormatException e) {
            log.error("Failed to parse total amount: {}", e.getMessage());
        }
        return null;
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