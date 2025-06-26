package com.receiptanalyzer.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.receiptanalyzer.model.FnQrData;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;

@Repository
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

    public void saveCheckData(FnQrData checkData) {
        String sql = "{CALL fiscal_data.save_check_data(?, ?, ?, ?, ?, ?, ?, ?)}";
        /*
        jdbcTemplate.update(sql,
                checkData.getClientId(),
                Timestamp.valueOf(checkData.getCheckDate()),
                checkData.getAmount(),
                checkData.getDeviceRegNumber(),
                checkData.getShiftNumber(),
                checkData.getCheckNumberInShift(),
                objectMapper.writeValueAsString(checkData.getItems()),
                objectMapper.writeValueAsString(checkData.getCategories())
        );

         */
        System.out.println("Сохранено");
    }
}
