package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageReference;

/**
 * An update states that a property of an object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public abstract class Update extends Marshallable implements Comparable<Update> {

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

	/**
	 * Yields the storage reference of the object whose field is modified.
	 * 
	 * @return the storage reference
	 */
	public final StorageReference getObject() {
		return object;
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
	 * Determines if this update is for the same property of the {@code other},
	 * although possibly for a different object. For instance, they are both class tags
	 * or they are both updates to the same field signature.
	 * 
	 * @param other the other update
	 */
	public abstract boolean sameProperty(Update other);

	/**
	 * Yields the size of this update, in terms of gas units consumed in store.
	 * 
	 * @param gasCostModel the model of gas costs
	 * @return the size
	 */
	public BigInteger size(GasCostModel gasCostModel) {
		return object.size(gasCostModel);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		object.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals an update from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the update
	 * @throws IOException if the update could not be unmarshalled
	 * @throws ClassNotFoundException if the update could not be unmarshalled
	 */
	public static Update from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		byte selector = context.readByte();
		switch (selector) {
		case ClassTag.SELECTOR: return new ClassTag(StorageReference.from(context), (ClassType) StorageType.from(context), TransactionReference.from(context));
		case UpdateOfBigInteger.SELECTOR_BALANCE: return new UpdateOfBigInteger(StorageReference.from(context), FieldSignature.BALANCE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR_GAS_PRICE: return new UpdateOfBigInteger(StorageReference.from(context), FieldSignature.GENERIC_GAS_STATION_GAS_PRICE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR_UBI_VALUE: return new UpdateOfBigInteger(StorageReference.from(context), FieldSignature.UNSIGNED_BIG_INTEGER_VALUE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR_RED_BALANCE: return new UpdateOfBigInteger(StorageReference.from(context), FieldSignature.RED_BALANCE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR_RED_BALANCE_TO_ZERO: return new UpdateOfBigInteger(StorageReference.from(context), FieldSignature.RED_BALANCE_FIELD, BigInteger.ZERO);
		case UpdateOfBigInteger.SELECTOR_NONCE: return new UpdateOfBigInteger(StorageReference.from(context), FieldSignature.EOA_NONCE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR: return new UpdateOfBigInteger(StorageReference.from(context), FieldSignature.from(context), context.readBigInteger());
		case UpdateOfBoolean.SELECTOR_FALSE: return new UpdateOfBoolean(StorageReference.from(context), FieldSignature.from(context), false);
		case UpdateOfBoolean.SELECTOR_TRUE: return new UpdateOfBoolean(StorageReference.from(context), FieldSignature.from(context), true);
		case UpdateOfByte.SELECTOR: return new UpdateOfByte(StorageReference.from(context), FieldSignature.from(context), context.readByte());
		case UpdateOfChar.SELECTOR: return new UpdateOfChar(StorageReference.from(context), FieldSignature.from(context), context.readChar());
		case UpdateOfDouble.SELECTOR: return new UpdateOfDouble(StorageReference.from(context), FieldSignature.from(context), context.readDouble());
		case UpdateOfEnumEager.SELECTOR: return new UpdateOfEnumEager(StorageReference.from(context), FieldSignature.from(context), context.readUTF(), context.readUTF());
		case UpdateOfEnumLazy.SELECTOR: return new UpdateOfEnumLazy(StorageReference.from(context), FieldSignature.from(context), context.readUTF(), context.readUTF());		
		case UpdateOfFloat.SELECTOR: return new UpdateOfFloat(StorageReference.from(context), FieldSignature.from(context), context.readFloat());
		case UpdateOfInt.SELECTOR: return new UpdateOfInt(StorageReference.from(context), FieldSignature.from(context), context.readInt());
		case UpdateOfInt.SELECTOR_SMALL: return new UpdateOfInt(StorageReference.from(context), FieldSignature.from(context), context.readShort());
		case UpdateOfInt.SELECTOR_VERY_SMALL: return new UpdateOfInt(StorageReference.from(context), FieldSignature.from(context), context.readByte());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_MAP_NODE_SIZE: return new UpdateOfInt(StorageReference.from(context), FieldSignature.STORAGE_TREE_MAP_NODE_SIZE_FIELD, context.readCompactInt());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE: return new UpdateOfInt(StorageReference.from(context), FieldSignature.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD, context.readCompactInt());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY: return new UpdateOfInt(StorageReference.from(context), FieldSignature.STORAGE_TREE_INTMAP_NODE_KEY_FIELD, context.readCompactInt());
		case UpdateOfLong.SELECTOR: return new UpdateOfLong(StorageReference.from(context), FieldSignature.from(context), context.readLong());
		case UpdateOfShort.SELECTOR: return new UpdateOfShort(StorageReference.from(context), FieldSignature.from(context), context.readShort());
		case UpdateOfStorage.SELECTOR: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.from(context), StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_LEFT: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_MAP_NODE_LEFT_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_MAP_NODE_RIGHT_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_KEY: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_MAP_NODE_KEY_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_VALUE: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_MAP_NODE_VALUE_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_ROOT: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_INTMAP_ROOT_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_INTMAP_NODE_VALUE_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_INTMAP_NODE_LEFT_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_ROOT: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.STORAGE_TREE_MAP_ROOT_FIELD, StorageReference.from(context));
		case UpdateOfStorage.SELECTOR_EVENT_CREATOR: return new UpdateOfStorage(StorageReference.from(context), FieldSignature.EVENT_CREATOR_FIELD, StorageReference.from(context));
		case UpdateOfString.SELECTOR_PUBLIC_KEY: return new UpdateOfString(StorageReference.from(context), FieldSignature.EOA_PUBLIC_KEY_FIELD, context.readUTF());
		case UpdateOfString.SELECTOR: return new UpdateOfString(StorageReference.from(context), FieldSignature.from(context), context.readUTF());
		case UpdateToNullEager.SELECTOR: return new UpdateToNullEager(StorageReference.from(context), FieldSignature.from(context));
		case UpdateToNullLazy.SELECTOR: return new UpdateToNullLazy(StorageReference.from(context), FieldSignature.from(context));
		default: throw new IOException("unexpected update selector: " + selector);
		}
	}
}