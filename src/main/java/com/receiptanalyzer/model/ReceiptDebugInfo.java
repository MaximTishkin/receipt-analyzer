package com.receiptanalyzer.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ReceiptDebugInfo {
    private ReceiptInfo receiptInfo;
    private String rawText;
    private String cleanedText;
} 