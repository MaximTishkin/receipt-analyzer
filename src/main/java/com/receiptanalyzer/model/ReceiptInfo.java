package com.receiptanalyzer.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@Builder
public class ReceiptInfo {
    private LocalDate date;
    private BigDecimal totalAmount;
    private Category category;

    public enum Category {
        GROCERIES("продукты"),
        CLOTHING("одежда"),
        CONSTRUCTION("стройматериалы");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
} 