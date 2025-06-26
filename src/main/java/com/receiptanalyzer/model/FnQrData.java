package com.receiptanalyzer.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Данные, полученные из QR-кода ФНС.
 */
public class FnQrData {
    private String dateTime;
    private String formattedDateTime;
    private String sum;
    private String fn;
    private String fiscalNumber;
    private String fiscalSign;
    private String docType;

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public void setFiscalSign(String fiscalSign) {
        this.fiscalSign = fiscalSign;
    }

    public void setFiscalNumber(String fiscalNumber) {
        this.fiscalNumber = fiscalNumber;
    }

    public void setFn(String fn) {
        this.fn = fn;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public void setFormattedDateTime(String formattedDateTime) {
        this.formattedDateTime = formattedDateTime;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getFormattedDateTime() {
        return formattedDateTime;
    }
}
