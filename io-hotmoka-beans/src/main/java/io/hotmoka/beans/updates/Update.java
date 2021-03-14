package io.hotmoka.beans.updates;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;
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
	 * @param ois the stream
	 * @return the update
	 * @throws IOException if the update could not be unmarshalled
	 * @throws ClassNotFoundException if the update could not be unmarshalled
	 */
	public static Update from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		switch (selector) {
		case ClassTag.SELECTOR: return new ClassTag(StorageReference.from(ois), (ClassType) StorageType.from(ois), TransactionReference.from(ois));
		case UpdateOfBigInteger.SELECTOR_BALANCE: return new UpdateOfBigInteger(StorageReference.from(ois), FieldSignature.BALANCE_FIELD, unmarshallBigInteger(ois));
		case UpdateOfBigInteger.SELECTOR_GAS_PRICE: return new UpdateOfBigInteger(StorageReference.from(ois), FieldSignature.GENERIC_GAS_STATION_GAS_PRICE_FIELD, unmarshallBigInteger(ois));
		case UpdateOfBigInteger.SELECTOR_UBI_VALUE: return new UpdateOfBigInteger(StorageReference.from(ois), FieldSignature.UNSIGNED_BIG_INTEGER_VALUE_FIELD, unmarshallBigInteger(ois));
		case UpdateOfBigInteger.SELECTOR_RED_BALANCE: return new UpdateOfBigInteger(StorageReference.from(ois), FieldSignature.RED_BALANCE_FIELD, unmarshallBigInteger(ois));
		case UpdateOfBigInteger.SELECTOR_NONCE: return new UpdateOfBigInteger(StorageReference.from(ois), FieldSignature.EOA_NONCE_FIELD, unmarshallBigInteger(ois));
		case UpdateOfBigInteger.SELECTOR: return new UpdateOfBigInteger(StorageReference.from(ois), FieldSignature.from(ois), unmarshallBigInteger(ois));
		case UpdateOfBoolean.SELECTOR_FALSE: return new UpdateOfBoolean(StorageReference.from(ois), FieldSignature.from(ois), false);
		case UpdateOfBoolean.SELECTOR_TRUE: return new UpdateOfBoolean(StorageReference.from(ois), FieldSignature.from(ois), true);
		case UpdateOfByte.SELECTOR: return new UpdateOfByte(StorageReference.from(ois), FieldSignature.from(ois), ois.readByte());
		case UpdateOfChar.SELECTOR: return new UpdateOfChar(StorageReference.from(ois), FieldSignature.from(ois), ois.readChar());
		case UpdateOfDouble.SELECTOR: return new UpdateOfDouble(StorageReference.from(ois), FieldSignature.from(ois), ois.readDouble());
		case UpdateOfEnumEager.SELECTOR: return new UpdateOfEnumEager(StorageReference.from(ois), FieldSignature.from(ois), ois.readUTF(), ois.readUTF());
		case UpdateOfEnumLazy.SELECTOR: return new UpdateOfEnumLazy(StorageReference.from(ois), FieldSignature.from(ois), ois.readUTF(), ois.readUTF());		
		case UpdateOfFloat.SELECTOR: return new UpdateOfFloat(StorageReference.from(ois), FieldSignature.from(ois), ois.readFloat());
		case UpdateOfInt.SELECTOR: return new UpdateOfInt(StorageReference.from(ois), FieldSignature.from(ois), ois.readInt());
		case UpdateOfInt.SELECTOR_SMALL: return new UpdateOfInt(StorageReference.from(ois), FieldSignature.from(ois), ois.readShort());
		case UpdateOfInt.SELECTOR_VERY_SMALL: return new UpdateOfInt(StorageReference.from(ois), FieldSignature.from(ois), ois.readByte());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_MAP_NODE_SIZE: return new UpdateOfInt(StorageReference.from(ois), FieldSignature.STORAGE_TREE_MAP_NODE_SIZE_FIELD, ois.readInt());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE: return new UpdateOfInt(StorageReference.from(ois), FieldSignature.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD, ois.readInt());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY: return new UpdateOfInt(StorageReference.from(ois), FieldSignature.STORAGE_TREE_INTMAP_NODE_KEY_FIELD, ois.readInt());
		case UpdateOfLong.SELECTOR: return new UpdateOfLong(StorageReference.from(ois), FieldSignature.from(ois), ois.readLong());
		case UpdateOfShort.SELECTOR: return new UpdateOfShort(StorageReference.from(ois), FieldSignature.from(ois), ois.readShort());
		case UpdateOfStorage.SELECTOR: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.from(ois), StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_LEFT: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_MAP_NODE_LEFT_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_MAP_NODE_RIGHT_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_KEY: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_MAP_NODE_KEY_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_VALUE: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_MAP_NODE_VALUE_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_ROOT: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_INTMAP_ROOT_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_INTMAP_NODE_VALUE_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_INTMAP_NODE_LEFT_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_ROOT: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.STORAGE_TREE_MAP_ROOT_FIELD, StorageReference.from(ois));
		case UpdateOfStorage.SELECTOR_EVENT_CREATOR: return new UpdateOfStorage(StorageReference.from(ois), FieldSignature.EVENT_CREATOR_FIELD, StorageReference.from(ois));
		case UpdateOfString.SELECTOR_PUBLIC_KEY: return new UpdateOfString(StorageReference.from(ois), FieldSignature.EOA_PUBLIC_KEY_FIELD, ois.readUTF());
		case UpdateOfString.SELECTOR: return new UpdateOfString(StorageReference.from(ois), FieldSignature.from(ois), ois.readUTF());
		case UpdateToNullEager.SELECTOR: return new UpdateToNullEager(StorageReference.from(ois), FieldSignature.from(ois));
		case UpdateToNullLazy.SELECTOR: return new UpdateToNullLazy(StorageReference.from(ois), FieldSignature.from(ois));
		default: throw new IOException("unexpected update selector: " + selector);
		}
	}
}