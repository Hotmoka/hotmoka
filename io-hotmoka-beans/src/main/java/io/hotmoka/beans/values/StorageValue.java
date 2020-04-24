package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import io.hotmoka.beans.internal.UnmarshallingUtils;
import io.hotmoka.beans.references.TransactionReference;

/**
 * A value that can be stored in the blockchain, passed as argument to an entry
 * or returned from an entry.
 */
public interface StorageValue extends Serializable, Comparable<StorageValue> {

	/**
	 * Marshals this value into the given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if the value cannot be marshalled
	 */
	void into(ObjectOutputStream oos) throws IOException;

	/**
	 * Factory method that unmarshals a value from the given stream.
	 * 
	 * @param ois the stream
	 * @return the value
	 * @throws IOException if the value could not be unmarshalled
	 * @throws ClassNotFoundException if the value could not be unmarshalled
	 */
	static StorageValue from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		switch (selector) {
		case BigIntegerValue.SELECTOR: return new BigIntegerValue(UnmarshallingUtils.unmarshallBigInteger(ois));
		case BooleanValue.SELECTOR_TRUE: return BooleanValue.TRUE;
		case BooleanValue.SELECTOR_FALSE: return BooleanValue.FALSE;
		case ByteValue.SELECTOR: return new ByteValue(ois.readByte());
		case CharValue.SELECTOR: return new CharValue(ois.readChar());
		case DoubleValue.SELECTOR: return new DoubleValue(ois.readDouble());
		case EnumValue.SELECTOR: return new EnumValue(ois.readUTF(), ois.readUTF());
		case FloatValue.SELECTOR: return new FloatValue(ois.readFloat());
		case IntValue.SELECTOR: return new IntValue(ois.readInt());
		case LongValue.SELECTOR: return new LongValue(ois.readLong());
		case NullValue.SELECTOR: return NullValue.INSTANCE;
		case ShortValue.SELECTOR: return new ShortValue(ois.readShort());
		case StorageReference.SELECTOR: return StorageReference.mk(TransactionReference.from(ois), UnmarshallingUtils.unmarshallBigInteger(ois));
		case StringValue.SELECTOR: return new StringValue(ois.readUTF());
		default: throw new IOException("unexpected value selector: " + selector);
		}
	}
}