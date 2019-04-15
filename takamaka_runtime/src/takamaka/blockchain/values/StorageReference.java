package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.TransactionReference;
import takamaka.lang.Immutable;
import takamaka.lang.Storage;

/**
 * A storage reference is a reference to an object that lives in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */

@Immutable
public final class StorageReference implements StorageValue {

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
	 * @param transaction The transaction that created the object.
	 * @param progressive The progressive number of the object among those that have been created
	 *                    during the same transaction.
	 */
	public StorageReference(TransactionReference transaction, BigInteger progressive) {
		this.transaction = transaction;
		this.progressive = progressive;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReference && ((StorageReference) other).progressive.equals(progressive) && ((StorageReference) other).transaction.equals(transaction);
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
	public Storage deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) {
		return blockchain.deserialize(classLoader, this);
	}

	@Override
	public String toString() {
		return transaction + "#" + progressive.toString(16);
	}
}