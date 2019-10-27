package takamaka.blockchain.values;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.runtime.AbstractStorage;

/**
 * A reference to an object of class type that is already stored in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */
@Immutable
public final class StorageReferenceAlreadyInBlockchain extends StorageReference {

	private static final long serialVersionUID = 5215119613321482697L;

	/**
	 * The transaction that created the object.
	 */
	public final TransactionReference transaction;

	/**
	 * Builds a storage reference.
	 * 
	 * @param transaction the transaction that created the object
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 */
	private StorageReferenceAlreadyInBlockchain(TransactionReference transaction, BigInteger progressive) {
		super(progressive);

		this.transaction = transaction;
	}

	private final static ConcurrentMap<StorageReferenceAlreadyInBlockchain, StorageReferenceAlreadyInBlockchain> cache = new ConcurrentHashMap<>();

	static StorageReferenceAlreadyInBlockchain mk(TransactionReference transaction, BigInteger progressive) {
		StorageReferenceAlreadyInBlockchain ref = new StorageReferenceAlreadyInBlockchain(transaction, progressive);
		return cache.computeIfAbsent(ref, __ -> ref);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReferenceAlreadyInBlockchain && super.equals(other) && ((StorageReferenceAlreadyInBlockchain) other).transaction.equals(transaction);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ transaction.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		diff = transaction.compareTo(((StorageReferenceAlreadyInBlockchain) other).transaction);
		if (diff != 0)
			return diff;

		return progressive.compareTo(((StorageReferenceAlreadyInBlockchain) other).progressive);
	}

	@Override
	public AbstractStorage deserialize(AbstractBlockchain blockchain) {
		return blockchain.deserialize(this);
	}

	@Override
	public String toString() {
		return transaction + super.toString();
	}

	@Override
	public StorageReferenceAlreadyInBlockchain contextualizeAt(TransactionReference where) {
		// this storage reference is absolute, it won't change
		return this;
	}

	@Override
	public String getClassName(AbstractBlockchain blockchain) {
		return blockchain.getClassNameOf(this);
	}

	@Override
	public BigInteger size() {
		return super.size().add(transaction.size());
	}
}