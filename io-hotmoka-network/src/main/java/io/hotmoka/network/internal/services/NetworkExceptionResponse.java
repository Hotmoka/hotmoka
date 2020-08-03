package io.hotmoka.network.internal.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import io.hotmoka.network.models.errors.ErrorModel;

/**
 * Wrapper class of {@link org.springframework.web.server.ResponseStatusException}
 * to throw exceptions and hence return to client a message with an HTTP status
 */
public class NetworkExceptionResponse extends ResponseStatusException {
	private static final long serialVersionUID = 1L;
	public final ErrorModel errorModel;

	NetworkExceptionResponse(HttpStatus status, ErrorModel errorModel) {
        super(status, errorModel.message != null ? errorModel.message : "");
        this.errorModel = errorModel;
    }

    @Override
    public String getMessage() {
        return getReason();
    }

    /**
     * Returns fully-qualified name of the class of the exception.
     */
    public String getExceptionType() {
	    return errorModel.exceptionType;
    }
}