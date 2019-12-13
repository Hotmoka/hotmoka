package io.takamaka.code.blockchain.values;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.GasCostModel;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A reference to an object of class type that can be stored in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */
@Immutable
public final class StorageReference implements StorageValue {

	private static final long serialVersionUID = 5215119613321482697L;

	/**
	 * The transaction that created the object.
	 */
	public final TransactionReference transaction;

	/**
	 * The progressive number of the object among those that have been created
	 * during the same transaction.
	 */
	public final BigInteger progressive;

	/**
	 * Builds a storage reference.
	 * 
	 * @param transaction the transaction that created the object
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 */
	private StorageReference(TransactionReference transaction, BigInteger progressive) {
		this.progressive = progressive;
		this.transaction = transaction;
	}

	private final static ConcurrentMap<StorageReference, StorageReference> cache = new ConcurrentHashMap<>();

	public static StorageReference mk(TransactionReference transaction, BigInteger progressive) {
		StorageReference ref = new StorageReference(transaction, progressive);
		return cache.computeIfAbsent(ref, __ -> ref);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReference &&
			((StorageReference) other).transaction.equals(transaction) &&
			((StorageReference) other).progressive.equals(progressive);
	}

	@Override
	public int hashCode() {
		return progressive.hashCode() ^ transaction.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		diff = transaction.compareTo(((StorageReference) other).transaction);
		if (diff != 0)
			return diff;

		return progressive.compareTo(((StorageReference) other).progressive);
	}

	@Override
	public Object deserialize(AbstractBlockchain blockchain) {
		return blockchain.deserialize(this);
	}

	@Override
	public String toString() {
		return transaction + "#" + progressive.toString(16);
	}

	/**
	 * Yields the name of the class of the object referenced by this reference.
	 * 
	 * @param blockchain the blockchain for which the deserialization is performed
	 * @return the name
	 */
	public String getClassName(AbstractBlockchain blockchain) {
		return blockchain.getClassNameOf(this);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot())
			.add(gasCostModel.storageCostOf(progressive)).add(transaction.size(gasCostModel));
	}
}