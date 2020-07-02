package io.hotmoka.network.service;

import io.hotmoka.network.exception.NodeNotFoundException;
import io.hotmoka.network.model.Error;
import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

public class NetworkService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    @Autowired
    protected ApplicationContext applicationContext;


    protected static ResponseEntity<Object> exceptionResponseOf(Exception e) {
        if (e instanceof NodeNotFoundException)
            return responseOf(new Error("Node instance not found"), HttpStatus.NOT_FOUND);
        else
            return responseOf(new Error("Application crashed..."), HttpStatus.BAD_REQUEST);
    }

    protected static void assertNodeNotNull(Node node) {
        if (node == null)
            throw new NodeNotFoundException();
    }

    private static ResponseEntity<Object> responseOf(Object o, HttpStatus httpStatus) {
        return new ResponseEntity<>(o, httpStatus);
    }

    protected static ResponseEntity<Object> okResponseOf(Object o) {
        return responseOf(o, HttpStatus.OK);
    }

    protected static ResponseEntity<Object> badRequestOf(Error error) {
        return responseOf(error, HttpStatus.BAD_REQUEST);
    }

    protected static ResponseEntity<Object> noContentResponse() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Map function to map the {@link io.hotmoka.nodes.Node} into a {@link org.springframework.http.ResponseEntity} through the use of a java {@link java.util.function.Function}
     * @param mapFunction the map function to apply
     * @return the result of the map function
     */
    protected ResponseEntity<Object> map(Function<Node, ResponseEntity<Object>> mapFunction) {
        try {

            Node node = (Node) this.applicationContext.getBean("node");
            assertNodeNotNull(node);

            return mapFunction.apply(node);

        } catch (Exception e) {
            LOGGER.error("Error occured during node mapping function", e);
            return exceptionResponseOf(e);
        }
    }
}
