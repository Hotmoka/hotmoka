package io.hotmoka.network.internal.services;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.network.internal.Application;
import io.hotmoka.network.internal.models.Error;
import io.hotmoka.nodes.Node;

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
     * Exception handler of the {@link io.hotmoka.nodes.Node} methods
     * @param e the exception to handle
     * @return a {@link org.springframework.http.ResponseEntity} with the wrapped error message
     */
    protected static ResponseEntity<Object> exceptionResponseOf(Exception e) {

        if (e instanceof TransactionRejectedException)
            return badRequestResponseOf(new Error("Transaction rejected"));

        if (e instanceof TransactionException)
            return badRequestResponseOf(new Error("Error during the transaction"));

        if (e instanceof CodeExecutionException)
            return badRequestResponseOf(new Error("Code execution error during the transaction"));

        return badRequestResponseOf(new Error(e.getMessage()));
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
     * Returns a {@link org.springframework.http.ResponseEntity} object with an {@link org.springframework.http.HttpStatus} of 400
     * @param error the body of the response
     * @return the {@link org.springframework.http.ResponseEntity}
     */
    protected static ResponseEntity<Object> badRequestResponseOf(Error error) {
        return responseOf(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Returns a {@link org.springframework.http.ResponseEntity} object with an {@link org.springframework.http.HttpStatus} of 404
     * @param error the body of the response
     * @return the {@link org.springframework.http.ResponseEntity}
     */
    protected static ResponseEntity<Object> notFoundResponseOf(Error error) {
        return responseOf(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Returns a {@link org.springframework.http.ResponseEntity} object with an {@link org.springframework.http.HttpStatus} of 204
     * @return the {@link org.springframework.http.ResponseEntity}
     */
    protected static ResponseEntity<Object> noContentResponse() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Wrap the exceptions that the given task may raise during the execution of the call
     * @param task the task to call
     * @return the result of the task
     */
    protected static ResponseEntity<Object> wrapExceptions(Callable<ResponseEntity<Object>> task) {
        try {
        	return task.call();
        }
        catch (Exception e) {
            LOGGER.error("Error occured during node mapping function", e);
            return exceptionResponseOf(e);
        }
    }
}
