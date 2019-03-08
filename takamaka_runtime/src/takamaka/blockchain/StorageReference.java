package takamaka.blockchain;

import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

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
	public final short progressive;

	/**
	 * Builds a storage reference.
	 * 
	 * @param transaction The transaction that created the object.
	 * @param progressive The progressive number of the object among those that have been created
	 *                    during the same transaction.
	 */
	public StorageReference(TransactionReference transaction, short progressive) {
		this.transaction = transaction;
		this.progressive = progressive;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReference && ((StorageReference) other).progressive == progressive && ((StorageReference) other).transaction.equals(transaction);
	}

	@Override
	public int hashCode() {
		return progressive ^ transaction.hashCode();
	}

	@Override
	public String toString() {
		return String.format("%s%04x", transaction, progressive);
	}
}