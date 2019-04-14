package takamaka.blockchain;

public class DeserializationError extends Error {
	public DeserializationError(Throwable cause) {
		super("Cannot deserialize value", cause);
	}
}
