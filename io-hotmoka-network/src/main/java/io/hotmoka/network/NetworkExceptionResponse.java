package io.hotmoka.network;

import io.hotmoka.network.errors.ErrorModel;

/**
 * A network exception with its message and HTTP status.
 */
public class NetworkExceptionResponse extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public final ErrorModel errorModel;
	private final String status;

	public NetworkExceptionResponse(String status, ErrorModel errorModel) {
        this.errorModel = errorModel;
        this.status = status;
    }

	public String getStatus() {
		return status;
	}

	/**
     * Returns the message of the exception.
     */
    @Override
    public String getMessage() {
        return errorModel.message != null ? errorModel.message : "";
    }

    /**
     * Returns the fully-qualified name of the class of the exception.
     */
    public String getExceptionClassName() {
	    return errorModel.exceptionClassName;
    }
}