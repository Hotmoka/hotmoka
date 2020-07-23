package io.hotmoka.network.exception;

public class ReferenceNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ReferenceNotFoundException() {
        super("Reference not found");
    }
}