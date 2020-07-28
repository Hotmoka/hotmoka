package io.hotmoka.network.internal.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Network exception interceptor to return a friendly error response to the client
 */
@RestControllerAdvice
public class NetworkExceptionInterceptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(NetworkExceptionInterceptor.class);

    @ExceptionHandler(value = NetworkExceptionResponse.class)
    public ResponseEntity<Error> handleNetworkException(NetworkExceptionResponse e) {
        if (e.getMessage() == null)
            return handleGenericException(e);

        return new ResponseEntity<>(new Error(e.getMessage()), e.getStatus());
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Error> handleGenericException(Exception e) {
        LOGGER.error("generic error occurred", e);
        return new ResponseEntity<>(new Error("Failed to process the request"), HttpStatus.BAD_REQUEST);
    }

    public static class Error {
        public final String message;

        public Error(String message) {
            this.message = message;
        }
    }
}
