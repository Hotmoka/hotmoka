package io.hotmoka.beans.references;

import java.io.Serializable;
import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;

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
}