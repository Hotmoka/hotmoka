package io.hotmoka.network.internal.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Wrapper class of {@link org.springframework.web.server.ResponseStatusException}
 * to throw exceptions and hence return to client a message with an HTTP status
 */
public class NetworkExceptionResponse extends ResponseStatusException {
	private static final long serialVersionUID = 1L;
	private final String exceptionType;

	NetworkExceptionResponse(HttpStatus status, String reason, String exceptionType) {
        super(status, reason);
        this.exceptionType = exceptionType;
    }

    @Override
    public String getMessage() {
        return getReason();
    }

    public String getExceptionType() {
        return exceptionType;
    }
}