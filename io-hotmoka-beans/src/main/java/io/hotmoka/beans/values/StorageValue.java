package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.ObjectInputStream;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.references.TransactionReference;

/**
 * A value that can be stored in the blockchain, passed as argument to an entry
 * or returned from an entry.
 */
public abstract class StorageValue extends Marshallable implements Comparable<StorageValue> {

	/**
	 * Factory method that unmarshals a value from the given stream.
	 * 
	 * @param ois the stream
	 * @return the value
	 * @throws IOException if the value could not be unmarshalled
	 * @throws ClassNotFoundException if the value could not be unmarshalled
	 */
	public static StorageValue from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		switch (selector) {
		case BigIntegerValue.SELECTOR: return new BigIntegerValue(unmarshallBigInteger(ois));
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
		case StorageReference.SELECTOR: return StorageReference.mk(TransactionReference.from(ois), unmarshallBigInteger(ois));
		case StringValue.SELECTOR: return new StringValue(ois.readUTF());
		default: {
			if (selector < 0)
				return new IntValue((selector + 256) - IntValue.SELECTOR);
			else
				return new IntValue(selector - IntValue.SELECTOR);
		}
		}
	}
}