package io.takamaka.code.engine;

/**
 * An exception thrown when a storage reference cannot be deserialized.
 */
@SuppressWarnings("serial")
public class DeserializationError extends Error {

	public DeserializationError(String message) {
		super(message);
	}

	public DeserializationError(Throwable cause) {
		super("Cannot deserialize value", cause);
	}
}
