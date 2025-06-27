package com.receiptanalyzer.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ReceiptData {
    private Long clientId;
    private LocalDateTime checkDate;
    private Double amount;
    private String deviceRegNumber;
    private Long shiftNumber;
    private Long checkNumberInShift;
    private List<ReceiptItem> items;
    private Map<String, CategoryStats> categories;

    // Геттеры и сеттеры
    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDateTime checkDate) {
        this.checkDate = checkDate;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDeviceRegNumber() {
        return deviceRegNumber;
    }

    public void setDeviceRegNumber(String deviceRegNumber) {
        this.deviceRegNumber = deviceRegNumber;
    }

    public Long getShiftNumber() {
        return shiftNumber;
    }

    public void setShiftNumber(Long shiftNumber) {
        this.shiftNumber = shiftNumber;
    }

    public Long getCheckNumberInShift() {
        return checkNumberInShift;
    }

    public void setCheckNumberInShift(Long checkNumberInShift) {
        this.checkNumberInShift = checkNumberInShift;
    }

    public List<ReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItem> items) {
        this.items = items;
    }

    public Map<String, CategoryStats> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, CategoryStats> categories) {
        this.categories = categories;
    }
}
