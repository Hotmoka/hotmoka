package io.hotmoka.network.model;

public class Error {
    private final String message;

    public Error(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
