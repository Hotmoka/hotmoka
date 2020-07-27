package io.hotmoka.network.internal.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Network exception interceptor to return a friendly error response to the client
 */
@RestControllerAdvice
public class NetworkExceptionInterceptor {

    @ExceptionHandler(value = NetworkExceptionResponse.class)
    public ResponseEntity<Error> handleNetworkException(NetworkExceptionResponse e) {
        if (e.getMessage() == null)
            return handleGenericException(e);

        Error error = new Error();
        error.setMessage(e.getMessage());
        return new ResponseEntity<>(error, e.getStatus());
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Error> handleGenericException(Exception e) {
        Error error = new Error();
        error.setMessage("Failed to process the request");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private static class Error {
        private String message;

        /**
         * Used by Spring.
         */
        @SuppressWarnings("unused")
		public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
