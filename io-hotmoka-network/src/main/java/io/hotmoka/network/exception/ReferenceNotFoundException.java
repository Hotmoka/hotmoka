package io.hotmoka.network.exception;

public class ReferenceNotFoundException extends RuntimeException {

    public ReferenceNotFoundException() {
        super("Reference not found");
    }
}
