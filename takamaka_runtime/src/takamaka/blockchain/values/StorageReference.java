package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.TransactionReference;
import takamaka.lang.Immutable;
import takamaka.lang.Storage;

/**
 * A reference to an object of class type that is already stored in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */
@Immutable
public final class StorageReference extends AbstractStorageReference {

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
	public StorageReference(TransactionReference transaction, BigInteger progressive) {
		super(progressive);

		this.transaction = transaction;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReference && super.equals(other) && ((StorageReference) other).transaction.equals(transaction);
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

		diff = transaction.compareTo(((StorageReference) other).transaction);
		if (diff != 0)
			return diff;

		return progressive.compareTo(((StorageReference) other).progressive);
	}

	@Override
	public Storage deserialize(AbstractBlockchain blockchain) {
		return blockchain.deserialize(this);
	}

	@Override
	public String toString() {
		return transaction + super.toString();
	}
}