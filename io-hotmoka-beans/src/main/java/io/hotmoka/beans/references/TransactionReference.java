package io.hotmoka.beans.references;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.internal.UnmarshallingUtils;

/**
 * A unique identifier for a transaction. This can be anything, from a
 * progressive number to a block/transaction pair to a database reference.
 * Each specific implementation of {@link io.hotmoka.nodes.Node}
 * provides its implementation of this interface. The order of comparison
 * is arbitrary, as long as it is a total order.
 */
@Immutable
public interface TransactionReference extends Serializable, Comparable<TransactionReference> {

	/**
	 * Yields the number of this transaction reference. It uniquely identifies the transaction
	 * inside the node that generated it.
	 * 
	 * @return the number
	 */
	BigInteger getNumber();

	/**
	 * Yields the subsequent transaction reference, that comes after this.
	 * 
	 * @return the subsequent transaction reference
	 */
	TransactionReference getNext();

	boolean equals(Object other);

	int hashCode();

	String toString();

	/**
	 * Marshals this transaction reference into the given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if the transaction reference cannot be marshalled
	 */
	void into(ObjectOutputStream oos) throws IOException;

	/**
	 * Factory method that unmarshals a transaction reference from the given stream.
	 * 
	 * @param ois the stream
	 * @return the transaction reference
	 * @throws IOException if the transaction reference could not be unmarshalled
	 * @throws ClassNotFoundException if the transaction reference could not be unmarshalled
	 */
	static TransactionReference from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new LocalTransactionReference(UnmarshallingUtils.unmarshallBigInteger(ois));
	}
}