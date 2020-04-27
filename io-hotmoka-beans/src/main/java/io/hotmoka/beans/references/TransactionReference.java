package io.hotmoka.beans.references;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.annotations.Immutable;

/**
 * A unique identifier for a transaction. This can be anything, from a
 * progressive number to a block/transaction pair to a database reference.
 * Each specific implementation of {@link io.hotmoka.nodes.Node}
 * provides its implementation of this interface. The order of comparison
 * is arbitrary, as long as it is a total order.
 */
@Immutable
public abstract class TransactionReference extends Marshallable implements Comparable<TransactionReference> {

	/**
	 * Yields the number of this transaction reference. It uniquely identifies the transaction
	 * inside the node that generated it.
	 * 
	 * @return the number
	 */
	public abstract BigInteger getNumber();

	/**
	 * Yields the subsequent transaction reference, that comes after this.
	 * 
	 * @return the subsequent transaction reference
	 */
	public abstract TransactionReference getNext();

	/**
	 * Factory method that unmarshals a transaction reference from the given stream.
	 * 
	 * @param ois the stream
	 * @return the transaction reference
	 * @throws IOException if the transaction reference could not be unmarshalled
	 * @throws ClassNotFoundException if the transaction reference could not be unmarshalled
	 */
	public static TransactionReference from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new LocalTransactionReference(unmarshallBigInteger(ois));
	}
}