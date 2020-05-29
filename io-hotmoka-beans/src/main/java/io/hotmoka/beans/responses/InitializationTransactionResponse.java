package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A response for a transaction that initializes a node.
 * After that, no more initial transactions can be executed.
 */
@Immutable
public class InitializationTransactionResponse extends InitialTransactionResponse {
	final static byte SELECTOR = 14;

	/**
	 * Builds the transaction response.
	 */
	public InitializationTransactionResponse() {
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InitializationTransactionResponse;
	}

	@Override
	public int hashCode() {
		return 13011973;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName();
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
	}
}