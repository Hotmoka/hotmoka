package io.hotmoka.remote.internal.websockets.client.stomp;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A simple class to represent the STOMP headers.
 */
public class StompHeaders {
    private final Map<String, String> headers = new HashMap<>();

    public void add(String key, String value) {
        headers.put(key, value);
    }

    public String getDestination() {
        String destination = headers.get("destination");
        if (destination == null)
            throw new NoSuchElementException("Destination not found");

        return destination;
    }

    @Override
    public String toString() {
        return "StompHeaders{headers=" + headers + '}';
    }
}
