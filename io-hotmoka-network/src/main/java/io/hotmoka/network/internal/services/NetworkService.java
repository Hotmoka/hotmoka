package io.hotmoka.network.internal.services;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.network.exception.NodeNotFoundException;
import io.hotmoka.network.internal.models.Error;
import io.hotmoka.network.internal.util.NodeFunction;
import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class NetworkService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * Exception handler of the {@link io.hotmoka.nodes.Node} methods
     * @param e the exception to handle
     * @return a {@link org.springframework.http.ResponseEntity} with the wrapped error message
     */
    protected static ResponseEntity<Object> exceptionResponseOf(Exception e) {

        if (e instanceof NodeNotFoundException)
            return notFoundResponseOf(new Error("Node instance not found"));

        if (e instanceof TransactionRejectedException)
            return badRequestResponseOf(new Error("Transaction rejected"));

        if (e instanceof TransactionException)
            return badRequestResponseOf(new Error("Error during the transaction"));

        if (e instanceof CodeExecutionException)
            return badRequestResponseOf(new Error("Code execution error during the transaction"));

        return badRequestResponseOf(new Error("Application crashed..."));
    }

    /**
     * Method to assert that a {@link io.hotmoka.nodes.Node} is not null
     * @param node the node to check
     */
    protected static void assertNodeNotNull(Node node) {
        if (node == null)
            throw new NodeNotFoundException();
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
     * Map function to map the {@link io.hotmoka.nodes.Node} into a {@link org.springframework.http.ResponseEntity} through the use of a {@link io.hotmoka.network.internal.util.NodeFunction}
     * @param nodeFunction the node function to apply
     * @return the result of the node function
     */
    protected ResponseEntity<Object> map(NodeFunction<Node, ResponseEntity<Object>> nodeFunction) {
        try {

            Node node = (Node) this.applicationContext.getBean("node");
            assertNodeNotNull(node);

            return nodeFunction.apply(node);

        } catch (Exception e) {
            LOGGER.error("Error occured during node mapping function", e);
            return exceptionResponseOf(e);
        }
    }
}
