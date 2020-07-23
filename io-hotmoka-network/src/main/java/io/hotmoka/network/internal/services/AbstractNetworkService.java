package io.hotmoka.network.internal.services;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.network.internal.Application;
import io.hotmoka.network.internal.models.function.Mapper;
import io.hotmoka.network.internal.services.exception.NetworkExceptionResponse;
import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.concurrent.Callable;

public abstract class AbstractNetworkService {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractNetworkService.class);

    private @Autowired Application application;

    /**
     * Yields the Hotmoka node exposed by the Spring application of this service.
     * 
     * @return the Hotmoka node
     */
    protected final Node getNode() {
    	return application.getNode();
    }

    protected final static byte[] decodeBase64(String what) {
    	return Base64.getDecoder().decode(what);
    }

    /**
     * It returns a response which is the result R of T by applying the mapper function
     * @param t the input T
     * @param mapper the mapper function
     * @param <T> the input type of the mapper function
     * @param <R> the result type of the mapper function
     * @return the result R of T of the mapper function
     */
    protected static <T, R> R responseOf(T t, Mapper<T, R> mapper) {
        return mapper.map(t);
    }

    /**
     * Returns a {@link org.springframework.http.ResponseEntity} object with an {@link org.springframework.http.HttpStatus} of 204.
     * 
     * @return the {@link org.springframework.http.ResponseEntity}
     */
    protected static ResponseEntity<Void> noContentResponse() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the result of a {@link java.util.concurrent.Callable} task,
     * wrapping its exceptions into a {@link NetworkExceptionResponse}.
	 *
     * @param task the task to call
     * @return the result of the task
     */
    protected static <T> T wrapExceptions(Callable<T> task) {
        try {
            return task.call();
        }
        catch (Exception e) {
        	LOGGER.error("error during network request", e);
        	throw networkExceptionFor(e);
        }
    }

	/**
	 * Yields a {@link NetworkExceptionResponse} for the given exception
	 * 
	 * @param e the exception to notify
	 */
	private static NetworkExceptionResponse networkExceptionFor(Exception e) throws NetworkExceptionResponse {
	    if (e instanceof TransactionRejectedException)
	        return new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, "Transaction rejected");
	    else if (e instanceof TransactionException)
	        return new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, "Transaction error");
	    else if (e instanceof CodeExecutionException)
	        return new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, "Code execution error during the transaction");
	    else
	    	return new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, e.getMessage());
	}
}