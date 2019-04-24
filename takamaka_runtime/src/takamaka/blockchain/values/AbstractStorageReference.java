package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.lang.Immutable;

/**
 * A reference to an object of class type that can be stored in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */
@Immutable
public abstract class AbstractStorageReference implements StorageValue {

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
	public AbstractStorageReference(BigInteger progressive) {
		this.progressive = progressive;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AbstractStorageReference && ((AbstractStorageReference) other).progressive.equals(progressive);
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

		return progressive.compareTo(((AbstractStorageReference) other).progressive);
	}

	@Override
	public String toString() {
		return "#" + progressive.toString(16);
	}
}