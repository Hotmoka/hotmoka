package io.hotmoka.network.internal.services;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.network.internal.Application;
import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.Callable;

public class NetworkService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    private @Autowired Application application;

    /**
     * Yields the Hotmoka node exposed by the Spring application of this service.
     * 
     * @return the Hotmoka node
     */
    protected final Node getNode() {
    	return application.getNode();
    }

    /**
     * It returns a {@link NetworkExceptionResponse} for the given exception
     * @param e the exception to wrap
     */
    protected static NetworkExceptionResponse networkExceptionFor(Exception e) throws NetworkExceptionResponse {

        if (e instanceof TransactionRejectedException)
            return new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, "Transaction rejected");

        if (e instanceof TransactionException)
            return new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, "Error during the transaction");

        if (e instanceof CodeExecutionException)
            return new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, "Code execution error during the transaction");

        return new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Creates a {@link org.springframework.http.ResponseEntity} object
     * @param o the body of the {@link org.springframework.http.ResponseEntity}
     * @param httpStatus the http status
     * @return a {@link org.springframework.http.ResponseEntity}
     */
    private static ResponseEntity<Object> responseOf(Object o, HttpStatus httpStatus) {
        return new ResponseEntity<>(o, httpStatus);
    }

    /**
     * Returns a {@link org.springframework.http.ResponseEntity} object with an {@link org.springframework.http.HttpStatus} of 200
     * @param o the body of the response
     * @return tye {@link org.springframework.http.ResponseEntity}
     */
    protected static ResponseEntity<Object> okResponseOf(Object o) {
        return responseOf(o, HttpStatus.OK);
    }

    /**
     * Returns a {@link org.springframework.http.ResponseEntity} object with an {@link org.springframework.http.HttpStatus} of 204
     * @return the {@link org.springframework.http.ResponseEntity}
     */
    protected static ResponseEntity<Object> noContentResponse() {
        return ResponseEntity.noContent().build();
    }

    /**
     * It returns the result of a {@link java.util.concurrent.Callable} task wrapped in a {@link org.springframework.http.ResponseEntity}
     * or it could throw a {@link NetworkExceptionResponse} to wrap
     * a possible exception that may raise during the execution of the task call
     * @param task the task to call
     * @return the result of the task
     */
    protected static ResponseEntity<Object> wrapExceptions(Callable<ResponseEntity<Object>> task) {
        try {
        	return task.call();
        }
        catch (Exception e) {
            LOGGER.error("Error occured during node mapping function", e);
            throw networkExceptionFor(e);
        }
    }
}
