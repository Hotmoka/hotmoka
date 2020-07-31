package io.hotmoka.network.internal.services;

import io.hotmoka.network.models.network.ErrorModel;
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

    /**
     * It return a {@link io.hotmoka.network.models.network.ErrorModel} from this exception response
     * @return the model
     */
    public ErrorModel toErrorModel() {
	    return new ErrorModel(this.getMessage(), this.exceptionType);
    }
}