package com.pizzashop.exception;

import org.springframework.http.HttpStatus;

public class OrderValidationException extends ApiException {

    public OrderValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
