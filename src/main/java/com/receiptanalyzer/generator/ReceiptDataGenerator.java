package com.receiptanalyzer.generator;

import com.receiptanalyzer.model.CategoryStats;
import com.receiptanalyzer.model.ReceiptItem;
import com.receiptanalyzer.model.ReceiptData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Генерация товаров и категорий для чека.
 * В целевом решении список товаров по чеку получаем из ФНС (напрямую или через OpenAPI).
 */
@Service
public class ReceiptDataGenerator {

    // Категории и примеры товаров
    private static final Map<String, List<String>> CATEGORY_PRODUCTS = Map.of(
            "Детские товары", List.of("Конструктор Лего", "Кукла", "Мяч", "Пазлы", "Раскраска"),
            "Автотовары", List.of("Щетки стеклоочистителя", "Автошампунь", "Антифриз", "Коврики", "Освежитель воздуха"),
            "Овощи/Фрукты", List.of("Огурцы", "Помидоры", "Яблоки", "Бананы", "Картофель"),
            "Молочная продукция", List.of("Молоко", "Кефир", "Творог", "Сметана", "Йогурт"),
            "Одежда", List.of("Футболка", "Джинсы", "Куртка", "Носки", "Шапка")
    );

    // Диапазоны цен для категорий (в рублях, с учетом копеек)
    private static final Map<String, double[]> CATEGORY_PRICE_RANGES = Map.of(
            "Детские товары", new double[]{300.0, 1500.0},
            "Автотовары", new double[]{200.0, 2500.0},
            "Овощи/Фрукты", new double[]{20.0, 150.0},
            "Молочная продукция", new double[]{30.0, 200.0},
            "Одежда", new double[]{150.0, 5000.0}
    );

    // Диапазоны количества
    private static final int[] QUANTITY_RANGE = {1, 5};

    // Минимум 5, максимум 10 товаров в чеке
    private static final int[] ITEMS_COUNT_RANGE = {5, 10};

    /**
     * Заполнить данные по чеку товарами и категориями.
     *
     * @param data данные по чеку
     */
    public void fillReceiptData(ReceiptData data) {
        List<ReceiptItem> items = new ArrayList<>();
        Map<String, CategoryStats> categories = new HashMap<>();

        // Случайное количество товаров в чеке
        int itemsCount = ThreadLocalRandom.current().nextInt(
                ITEMS_COUNT_RANGE[0], ITEMS_COUNT_RANGE[1] + 1);

        for (int i = 0; i < itemsCount; i++) {
            // Выбираем случайную категорию
            String category = getRandomCategory();
            String productName = getRandomProduct(category);

            // Генерируем цену и количество
            double price = getRandomPrice(category);
            int quantity = ThreadLocalRandom.current().nextInt(
                    QUANTITY_RANGE[0], QUANTITY_RANGE[1] + 1);

            // Создаем товар
            items.add(new ReceiptItem(productName, price, quantity));

            // Обновляем статистику по категории
            updateCategoryStats(categories, category, price, quantity);
        }

        // Рассчитываем средние цены
        calculateAveragePrices(categories);
        data.setItems(items);
        data.setCategories(categories);
    }

    private static String getRandomCategory() {
        List<String> keys = new ArrayList<>(CATEGORY_PRODUCTS.keySet());
        return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
    }

    private static String getRandomProduct(String category) {
        List<String> products = CATEGORY_PRODUCTS.get(category);
        return products.get(ThreadLocalRandom.current().nextInt(products.size()));
    }

    private static double getRandomPrice(String category) {
        double[] range = CATEGORY_PRICE_RANGES.get(category);
        // Генерируем цену с двумя знаками после запятой (копейки)
        return Math.round(ThreadLocalRandom.current().nextDouble(range[0], range[1]) * 100) / 100.0;
    }

    private static void updateCategoryStats(
            Map<String, CategoryStats> categories,
            String category,
            double price,
            int quantity) {

        CategoryStats stats = categories.getOrDefault(category,
                new CategoryStats(0.0, 0, 0.0));

        stats.setTotalAmount(stats.getTotalAmount() + price * quantity);
        stats.setCount(stats.getCount() + quantity);

        categories.put(category, stats);
    }

    private static void calculateAveragePrices(Map<String, CategoryStats> categories) {
        categories.forEach((category, stats) -> {
            if (stats.getCount() > 0) {
                double avg = stats.getTotalAmount() / stats.getCount();
                stats.setAveragePrice(Math.round(avg * 100.0) / 100.0); // округление до 2 знаков
            }
        });
    }
}
