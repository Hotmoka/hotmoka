package takamaka.blockchain;

import java.io.Serializable;
import java.math.BigInteger;

import io.takamaka.code.annotations.Immutable;
import takamaka.blockchain.values.StorageReference;

/**
 * An update states that a property of an object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public abstract class Update implements Serializable, Comparable<Update> {

	private static final long serialVersionUID = 1921751386937488337L;

	/**
	 * The storage reference of the object whose field is modified.
	 */
	public final StorageReference object;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 */
	protected Update(StorageReference object) {
		this.object = object;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Update && ((Update) other).object.equals(object);
	}

	@Override
	public int hashCode() {
		return object.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return object.compareTo(other.object);
	}

	/**
	 * Yields an update derived from this, by assuming that the current transaction
	 * is the given one.
	 * 
	 * @param where the transaction
	 * @return the resulting update
	 */
	public abstract Update contextualizeAt(TransactionReference where);

	/**
	 * Determines if the information expressed by this update is set immediately
	 * when a storage object is deserialized from blockchain. Otherwise, the
	 * information will only be set on-demand.
	 * 
	 * @return true if and only if the information is eager
	 */
	public boolean isEager() {
		return true; // subclasses may redefine
	}

	/**
	 * Determines if this update carries information for the same property as another.
	 * 
	 * @param other the other update
	 * @return true if and only if that condition holds
	 */
	public boolean isForSamePropertyAs(Update other) {
		return getClass() == other.getClass() && object.equals(other.object);
	}

	/**
	 * Yields a measure of this update, to be used to assess its gas cost
	 * when stored in blockchain.
	 * 
	 * @return the size of this update. This must be positive
	 */
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(object.size());
	};
}