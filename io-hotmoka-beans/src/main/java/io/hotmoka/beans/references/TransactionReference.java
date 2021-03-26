package io.hotmoka.beans.references;

import java.io.IOException;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;

/**
 * A unique identifier for a transaction.
 */
@Immutable
public abstract class TransactionReference extends Marshallable implements Comparable<TransactionReference> {

	/**
	 * Yields the hash of the request that generated the transaction.
	 * 
	 * @return the hash
	 */
	public abstract String getHash();

	/**
	 * Yields the hash of the request, as an array of bytes.
	 * 
	 * @return the hash
	 */
	public abstract byte[] getHashAsBytes();

	/**
	 * Factory method that unmarshals a transaction reference from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the transaction reference
	 * @throws IOException if the transaction reference could not be unmarshalled
	 * @throws ClassNotFoundException if the transaction reference could not be unmarshalled
	 */
	public static TransactionReference from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		return context.readTransactionReference();
	}
}