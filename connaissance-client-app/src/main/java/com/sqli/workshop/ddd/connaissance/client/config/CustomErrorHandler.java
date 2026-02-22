package com.sqli.workshop.ddd.connaissance.client.config;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import com.sqli.workshop.ddd.connaissance.client.domain.exceptions.AdresseInvalideException;

import java.io.IOException;

@ControllerAdvice
/**
 * CustomErrorHandler - TODO: description
 *
 * @todo Add detailed Javadoc
 */
public class CustomErrorHandler {

    @ExceptionHandler(ConstraintViolationException.class)
/**
 * handleConstraintViolationException() - TODO: description
 *
 * @todo Add detailed Javadoc
 */
    public void handleConstraintViolationException(ConstraintViolationException exception,
            ServletWebRequest webRequest) throws IOException {
        webRequest.getResponse().sendError(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }
    @ExceptionHandler(AdresseInvalideException.class)
/**
 * handleAdresseInvalideException() - TODO: description
 *
 * @todo Add detailed Javadoc
 */
    public void handleAdresseInvalideException(AdresseInvalideException exception,
            ServletWebRequest webRequest) throws IOException {
        webRequest.getResponse().sendError(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }
}