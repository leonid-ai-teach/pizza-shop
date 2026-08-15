package com.pizzashop.exception;

import org.springframework.http.HttpStatus;

/**
 * Wrong email or wrong password. Deliberately one exception for both cases: telling a caller
 * which half was wrong turns the login form into an oracle for which admin accounts exist.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
    }
}
