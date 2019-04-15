package takamaka.blockchain;

/**
 * An exception thrown when a storage reference cannot be deserialized.
 */
public class DeserializationError extends Error {
	public DeserializationError(Throwable cause) {
		super("Cannot deserialize value", cause);
	}
}
