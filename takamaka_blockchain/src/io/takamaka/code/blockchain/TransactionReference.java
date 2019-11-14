package io.takamaka.code.blockchain;

import java.io.Serializable;
import java.math.BigInteger;

import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A unique identifier for a transaction. This can be anything, from a
 * progressive number to a block/transaction pair to a database reference.
 * Each specific implementation of {@link io.takamaka.code.blockchain.Blockchain}
 * provides its implementation of this class.
 * They must be comparable. The order of comparison is not relevant.
 */
@Immutable
public interface TransactionReference extends Serializable, Comparable<TransactionReference> {

	/**
	 * Determines if this transaction reference precedes the other one in the blockchain.
	 * 
	 * @param other the other blockchain reference
	 * @return true if and only if that condition holds
	 */
	boolean isOlderThan(TransactionReference other);

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