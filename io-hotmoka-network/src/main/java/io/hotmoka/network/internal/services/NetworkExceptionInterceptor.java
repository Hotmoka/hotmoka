package io.hotmoka.network.internal.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.hotmoka.network.models.errors.ErrorModel;

/**
 * Network exception interceptor to return a friendly error response to the client
 */
@RestControllerAdvice
public class NetworkExceptionInterceptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(NetworkExceptionInterceptor.class);

    @ExceptionHandler(value = NetworkExceptionResponse.class)
    public ResponseEntity<ErrorModel> handleNetworkException(NetworkExceptionResponse e) {
        return new ResponseEntity<>(e.errorModel, e.getStatus());
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorModel> handleGenericException(Exception e) {
        LOGGER.error("a generic error occurred", e);
       	return new ResponseEntity<>(new ErrorModel("Failed to process the request",  e.getClass().getName()), HttpStatus.BAD_REQUEST);
    }
}
