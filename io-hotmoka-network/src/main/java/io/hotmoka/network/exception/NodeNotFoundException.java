package io.hotmoka.network.exception;

public class NodeNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NodeNotFoundException() {
        super("Node instance not found");
    }
}
