package takamaka.lang;

/**
 * An exception thrown when a potentially white-listed method is called
 * with a parameter (or receiver) that makes it non-white-listed.
 */
@SuppressWarnings("serial")
public class NotWhiteListedCallException extends IllegalStateException {
	public NotWhiteListedCallException(String message) {
		super(message);
	}
}