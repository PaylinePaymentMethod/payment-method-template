package com.payline.payment.template.utils;

public class BadFieldException extends InvalidRequestException {
    private final String field;

    public BadFieldException(String  field,String s) {
        super(s);
        this.field = field;
    }

    public String getField() {
        return field;
    }

}
