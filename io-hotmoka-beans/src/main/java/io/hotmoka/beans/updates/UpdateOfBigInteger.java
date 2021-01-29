package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that the {@link java.math.BigInteger}
 * field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfBigInteger extends UpdateOfField {
	final static byte SELECTOR = 2;
	final static byte SELECTOR_BALANCE = 1;
	final static byte SELECTOR_NONCE = 12;
	final static byte SELECTOR_RED_BALANCE = 13;
	final static byte SELECTOR_RGNONCE = 14;

	/**
	 * The new value of the field.
	 */
	public final BigInteger value;

	/**
	 * Builds an update of a {@link java.math.BigInteger} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfBigInteger(StorageReference object, FieldSignature field, BigInteger value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new BigIntegerValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfBigInteger && super.equals(other) && ((UpdateOfBigInteger) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfBigInteger) other).value);
	}

	@Override
	public boolean isEager() {
		// a lazy BigInteger could be stored into a lazy Object or Serializable or Comparable or Number field
		return field.type.equals(ClassType.BIG_INTEGER);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(value));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (FieldSignature.BALANCE_FIELD.equals(field)) {
			context.oos.writeByte(SELECTOR_BALANCE);
			super.intoWithoutField(context);
		}
		else if (FieldSignature.RED_BALANCE_FIELD.equals(field)) {
			context.oos.writeByte(SELECTOR_RED_BALANCE);
			super.intoWithoutField(context);
		}
		else if (FieldSignature.EOA_NONCE_FIELD.equals(field)) {
			context.oos.writeByte(SELECTOR_NONCE);
			super.intoWithoutField(context);
		}
		else if (FieldSignature.RGEOA_NONCE_FIELD.equals(field)) {
			context.oos.writeByte(SELECTOR_RGNONCE);
			super.intoWithoutField(context);
		}
		else {
			context.oos.writeByte(SELECTOR);
			super.into(context);
		}

		marshal(value, context);
	}
}