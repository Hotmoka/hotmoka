package io.takamaka.code.blockchain;

import java.io.Serializable;
import java.math.BigInteger;

import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A unique identifier for a transaction. This can be anything, from a
 * progressive number to a block/transaction pair to a database reference.
 * Each specific implementation of {@link io.takamaka.code.blockchain.Blockchain}
 * provides its implementation of this interface. The order of comparison
 * is arbitrary, as long as it is a total order.
 */
@Immutable
public interface TransactionReference extends Serializable, Comparable<TransactionReference> {

	/**
	 * Yields a measure of this update, to be used to assess its gas cost
	 * when stored in blockchain.
	 * 
	 * @return the size of this update. This must be positive
	 */
	BigInteger size();

	boolean equals(Object other);

	int hashCode();

	String toString();
}