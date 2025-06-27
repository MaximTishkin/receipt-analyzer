package com.receiptanalyzer.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.receiptanalyzer.exception.RepositoryException;
import com.receiptanalyzer.model.ReceiptData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class ReceiptRepository {
    private static final String SAVE_RECEIPT_DATA_SQL = "CALL fiscal_data.save_check_data(?, ?::timestamp without time zone, ?::numeric, ?, ?, ?, ?::jsonb, ?::jsonb)";

    private final JdbcTemplate jdbcTemplate;
    private ObjectMapper mapper;

    @PostConstruct
    public void init() {
        mapper = new ObjectMapper();
    }

    @Autowired
    public ReceiptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveReceiptData(ReceiptData receiptData) throws RepositoryException {
        log.info("Сохранение чека: " + receiptData);
        try {
            jdbcTemplate.update(SAVE_RECEIPT_DATA_SQL,
                    receiptData.getClientId(),
                    receiptData.getCheckDate(),
                    receiptData.getAmount(),
                    receiptData.getDeviceRegNumber(),
                    receiptData.getShiftNumber(),
                    receiptData.getCheckNumberInShift(),
                    receiptData.getItems() != null ? toJson(receiptData.getItems()) : "{}",
                    receiptData.getCategories() != null ? toJson(receiptData.getCategories()) : "{}"
            );
        } catch (Exception e) {
            throw new RepositoryException("Ошибка при сохранении данных чека", e);
        }
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка при сериализации в JSON", e.getMessage());
            return "{}";
        }
    }
}
