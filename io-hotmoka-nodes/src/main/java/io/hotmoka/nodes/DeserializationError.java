package io.hotmoka.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An exception thrown when a storage reference cannot be deserialized.
 */
@SuppressWarnings("serial")
public class DeserializationError extends Error {
	private final static Logger logger = LoggerFactory.getLogger(DeserializationError.class);

	public DeserializationError(String message) {
		super(message);
		logger.error(message, this);
	}

	public DeserializationError(Throwable cause) {
		super("Cannot deserialize value", cause);
		logger.error(getMessage(), this);
	}
}
