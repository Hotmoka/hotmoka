package io.hotmoka.beans.references;

import java.io.IOException;
import java.io.ObjectInputStream;

import io.hotmoka.beans.Marshallable;
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
	 * Factory method that unmarshals a transaction reference from the given stream.
	 * 
	 * @param ois the stream
	 * @return the transaction reference
	 * @throws IOException if the transaction reference could not be unmarshalled
	 * @throws ClassNotFoundException if the transaction reference could not be unmarshalled
	 */
	public static TransactionReference from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new LocalTransactionReference((String) ois.readObject());
	}
}