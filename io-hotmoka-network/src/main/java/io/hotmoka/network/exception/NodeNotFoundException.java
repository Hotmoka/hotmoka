package io.hotmoka.network.exception;

public class NodeNotFoundException extends RuntimeException {

    public NodeNotFoundException() {
        super("Node instance not found");
    }
}
