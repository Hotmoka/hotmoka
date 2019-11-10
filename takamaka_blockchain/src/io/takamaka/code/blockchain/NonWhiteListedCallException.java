package io.takamaka.code.blockchain;

/**
 * An exception thrown when a potentially white-listed method is called
 * with a parameter (or receiver) that makes it non-white-listed.
 */
@SuppressWarnings("serial")
public class NonWhiteListedCallException extends IllegalStateException {
	public NonWhiteListedCallException(String message) {
		super(message);
	}
}