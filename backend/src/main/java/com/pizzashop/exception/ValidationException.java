package com.pizzashop.exception;

import org.springframework.http.HttpStatus;

/** A request that is well-formed but violates a business rule. */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
