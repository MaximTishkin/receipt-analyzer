package com.receiptanalyzer.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.receiptanalyzer.exception.RepositoryException;
import com.receiptanalyzer.model.FiscalCheckData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class FiscalCheckRepository {
    private final JdbcTemplate jdbcTemplate;
    private ObjectMapper mapper;

    @PostConstruct
    public void init() {
        mapper = new ObjectMapper();
    }

    @Autowired
    public FiscalCheckRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveCheckData(FiscalCheckData checkData) throws RepositoryException {
        String sql = "CALL fiscal_data.save_check_data(?, ?::timestamp without time zone, ?::numeric, ?, ?, ?, ?::jsonb, ?::jsonb)";

        try {
            jdbcTemplate.update(sql,
                    checkData.getClientId() != null ? checkData.getClientId() : 0,
                    checkData.getCheckDate(),
                    checkData.getAmount(),
                    checkData.getDeviceRegNumber(),
                    checkData.getShiftNumber(),
                    checkData.getCheckNumberInShift(),
                    checkData.getItems() != null ? toJson(checkData.getItems()) : "{}",
                    checkData.getCategories() != null ? toJson(checkData.getCategories()) : "{}"
            );
        } catch (Exception e) {
            log.error("Ошибка при сохранении данных чека", e.getMessage());
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
