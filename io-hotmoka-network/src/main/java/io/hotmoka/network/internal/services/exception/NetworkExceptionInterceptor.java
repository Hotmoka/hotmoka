package io.hotmoka.network.internal.services.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Network exception interceptor to return a friendly error response to the client
 */
@RestControllerAdvice
public class NetworkExceptionInterceptor {

    @ExceptionHandler(value = NetworkExceptionResponse.class)
    public ResponseEntity<Error> handleGenericException(NetworkExceptionResponse e) {

        Error error = new Error();
        error.setMessage(e.getMessage());
        return new ResponseEntity<>(error, e.getStatus());
    }

    private static class Error {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
