package io.hotmoka.beans.references;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.internal.MarshallingUtils;

/**
 * A transaction reference that refers to a transaction in the local store of a node.
 */
public final class LocalTransactionReference implements TransactionReference {
	private static final long serialVersionUID = 5911713300386882185L;

	public final BigInteger number;

	/**
	 * The initial local transaction reference.
	 */
	public final static LocalTransactionReference FIRST = new LocalTransactionReference(BigInteger.ZERO);

	/**
	 * Builds a transaction reference.
	 * 
	 * @param number the number of the transaction
	 */
	LocalTransactionReference(BigInteger number) {
		this.number = number;
	}

	/**
	 * Builds a transaction reference.
	 * 
	 * @param number the number of the transaction
	 */
	public LocalTransactionReference(String s) {
		this.number = new BigInteger(s, 16);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TransactionReference &&
			((TransactionReference) other).getNumber().equals(number);
	}

	@Override
	public int hashCode() {
		return number.hashCode();
	}

	@Override
	public String toString() {
		return number.toString(16);
	}

	@Override
	public int compareTo(TransactionReference other) {
		return number.compareTo(other.getNumber());
	}

	@Override
	public LocalTransactionReference getNext() {
		return new LocalTransactionReference(number.add(BigInteger.ONE));
	}

	@Override
	public BigInteger getNumber() {
		return number;
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		MarshallingUtils.marshal(number, oos);
	}
}