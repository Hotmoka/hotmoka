package io.takamaka.code.blockchain;

import java.math.BigInteger;

import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.values.BooleanValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;

/**
 * An update of a field states that a boolean field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfBoolean extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2226960173435837206L;

	/**
	 * The new value of the field.
	 */
	private final boolean value;

	/**
	 * Builds an update of an {@code boolean} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfBoolean(StorageReference object, FieldSignature field, boolean value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new BooleanValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfBoolean && super.equals(other) && ((UpdateOfBoolean) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Boolean.hashCode(value);
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Boolean.compare(value, ((UpdateOfBoolean) other).value);
	}

	@Override
	public UpdateOfBoolean contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfBoolean(objectContextualized, field, value);
		else
			return this;
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.STORAGE_COST_PER_SLOT);
	}
}