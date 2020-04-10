package io.hotmoka.nodes;

/**
 * An exception thrown when a transaction has not enough gas
 * to complete its computation.
 */
@SuppressWarnings("serial")
public class OutOfGasError extends Error {

	public OutOfGasError() {
		super();
	}

	public OutOfGasError(String message) {
		super(message);
	}
}