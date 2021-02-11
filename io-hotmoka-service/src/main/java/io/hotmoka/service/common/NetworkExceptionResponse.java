package io.hotmoka.service.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import io.hotmoka.service.models.errors.ErrorModel;

/**
 * Wrapper class of {@link org.springframework.web.server.ResponseStatusException}
 * to throw exceptions and hence return to client a message with an HTTP status
 */
public class NetworkExceptionResponse extends ResponseStatusException {
	private static final long serialVersionUID = 1L;
	public final ErrorModel errorModel;

	public NetworkExceptionResponse(HttpStatus status, ErrorModel errorModel) {
        super(status, errorModel.message != null ? errorModel.message : "");
        this.errorModel = errorModel;
    }

    public NetworkExceptionResponse(ErrorModel errorModel) {
        this(HttpStatus.BAD_REQUEST, errorModel);
    }

    /**
     * Returns the message of the exception.
     */
    @Override
    public String getMessage() {
        return getReason() != null ? getReason() : "";
    }

    /**
     * Returns the fully-qualified name of the class of the exception.
     */
    public String getExceptionClassName() {
	    return errorModel.exceptionClassName;
    }
}