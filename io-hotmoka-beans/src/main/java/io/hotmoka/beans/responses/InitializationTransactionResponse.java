package io.hotmoka.beans.responses;

import java.io.IOException;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
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
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
     */
	public static InitializationTransactionResponse from(UnmarshallingContext context) {
		return new InitializationTransactionResponse();
	}
}