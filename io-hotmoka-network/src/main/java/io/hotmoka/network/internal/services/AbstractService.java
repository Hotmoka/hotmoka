package io.hotmoka.network.internal.services;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.network.internal.Application;
import io.hotmoka.network.models.errors.ErrorModel;
import io.hotmoka.nodes.Node;

abstract class AbstractService {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);

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
        catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
        	throw new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, new ErrorModel(e));
        }
        catch (Exception e) {
        	LOGGER.error("error during network request", e);
        	throw new NetworkExceptionResponse(HttpStatus.BAD_REQUEST, new ErrorModel(e));
        }
    }
}