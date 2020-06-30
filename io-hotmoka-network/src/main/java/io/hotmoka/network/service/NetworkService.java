package io.hotmoka.network.service;

import io.hotmoka.network.exception.NodeNotFoundException;
import io.hotmoka.network.model.Error;
import io.hotmoka.nodes.Node;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class NetworkService {


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

    protected static ResponseEntity<Object> responseOf(Object o, HttpStatus httpStatus) {
        return new ResponseEntity<>(o, httpStatus);
    }

    protected static ResponseEntity<Object> responseOf(Object o) {
        return responseOf(o, HttpStatus.OK);
    }

}
