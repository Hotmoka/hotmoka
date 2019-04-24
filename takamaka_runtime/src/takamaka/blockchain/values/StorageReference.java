package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.TransactionReference;
import takamaka.lang.Immutable;
import takamaka.lang.Storage;

/**
 * A reference to an object of class type that can be stored in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */
@Immutable
public abstract class StorageReference implements StorageValue {

	private static final long serialVersionUID = 5215119613321482697L;

	/**
	 * The progressive number of the object among those that have been created
	 * during the same transaction.
	 */
	public final BigInteger progressive;

	/**
	 * Builds a storage reference.
	 * 
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 */
	public StorageReference(BigInteger progressive) {
		this.progressive = progressive;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReference && ((StorageReference) other).progressive.equals(progressive);
	}

	@Override
	public int hashCode() {
		return progressive.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		return progressive.compareTo(((StorageReference) other).progressive);
	}

	@Override
	public String toString() {
		return "#" + progressive.toString(16);
	}

	@Override
	public abstract Storage deserialize(AbstractBlockchain blockchain);

	/**
	 * Yields the storage reference that corresponds to this, assuming that the
	 * current transaction is the given one.
	 * 
	 * @param where the current transaction
	 * @return the resulting storage reference
	 */
	public abstract GenericStorageReference contextualizeAt(TransactionReference where);
}