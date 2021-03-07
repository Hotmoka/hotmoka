package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that an integer field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfInt extends UpdateOfField {
	final static byte SELECTOR = 20;
	final static byte SELECTOR_SMALL = 21;
	final static byte SELECTOR_VERY_SMALL = 22;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE_SIZE = 27;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE = 29;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY = 30;

	/**
	 * The new value of the field.
	 */
	public final int value;

	/**
	 * Builds an update of an {@code int} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfInt(StorageReference object, FieldSignature field, int value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new IntValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfInt && super.equals(other) && ((UpdateOfInt) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value;
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Integer.compare(value, ((UpdateOfInt) other).value);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (FieldSignature.STORAGE_TREE_MAP_NODE_SIZE_FIELD.equals(field)) {
			context.oos.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE_SIZE);
			intoWithoutField(context);
			context.oos.writeInt(value);
		}
		else if (FieldSignature.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD.equals(field)) {
			context.oos.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE);
			intoWithoutField(context);
			context.oos.writeInt(value);
		}
		else if (FieldSignature.STORAGE_TREE_INTMAP_NODE_KEY_FIELD.equals(field)) {
			context.oos.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY);
			intoWithoutField(context);
			context.oos.writeInt(value);
		}
		else {
			boolean isSmall = ((short) value) == value;
			boolean isVerySmall = ((byte) value) == value;

			if (isVerySmall)
				context.oos.writeByte(SELECTOR_VERY_SMALL);
			else if (isSmall)
				context.oos.writeByte(SELECTOR_SMALL);
			else
				context.oos.writeByte(SELECTOR);

			super.into(context);

			if (isVerySmall)
				context.oos.writeByte((byte) value);
			else if (isSmall)
				context.oos.writeShort((short) value);
			else
				context.oos.writeInt(value);
		}
	}
}