package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.DeserializationError;
import takamaka.lang.Immutable;
import takamaka.lang.Storage;

/**
 * A reference to an object of class type that is not yet stored in the blockchain,
 * since it has been created during the current transaction.
 * Objects created during the same transaction are disambiguated by a progressive number.
 */
@Immutable
public final class StorageReferenceInCurrentTransaction extends AbstractStorageReference {

	private static final long serialVersionUID = -9199432347895285763L;

	/**
	 * Builds a storage reference to an object that is not yet stored in the blockchain,
	 * since it has been created during the current transaction.
	 *
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 */
	public StorageReferenceInCurrentTransaction(BigInteger progressive) {
		super(progressive);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReferenceInCurrentTransaction && super.equals(other);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		return progressive.compareTo(((StorageReferenceInCurrentTransaction) other).progressive);
	}

	@Override
	public Storage deserialize(AbstractBlockchain blockchain) {
		// if the object is not yet in blockchain, it is not possible to deserialize it
		throw new DeserializationError("This reference identifies an object not yet in blockchain");
	}

	@Override
	public String toString() {
		return "THIS_TRANSACTION" + super.toString();
	}
}