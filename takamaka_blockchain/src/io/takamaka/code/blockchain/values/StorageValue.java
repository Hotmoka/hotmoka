package io.takamaka.code.blockchain.values;

import java.io.Serializable;
import java.math.BigInteger;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.runtime.AbstractStorage;

/**
 * A value that can be stored in the blockchain, passed as argument to an entry
 * or returned from an entry.
 */
public interface StorageValue extends Serializable, Comparable<StorageValue> {

	/**
	 * Yields the deserialization of this value. That is, it yields an actual object in RAM
	 * that reflects its representation in blockchain.
	 * 
	 * @param blockchain the blockchain for which the deserialization is performed
	 * @return the deserialization
	 */
	Object deserialize(AbstractBlockchain blockchain);

	/**
	 * Yields the serialization of the given objects, that is, yields its
	 * representation in blockchain.
	 * 
	 * @param object the object to serialize. This must be a storage object, a Java wrapper
	 *               object for numerical types or a special Java object that is allowed
	 *               in blockchain, such as a {@link java.lang.String} or {@link java.math.BigInteger}
	 * @return the serialization of {@code object}, if any
	 * @throws IllegalArgumentException if the type of {@code object} is not allowed in blockchain
	 */
	static StorageValue serialize(Object object) throws IllegalArgumentException {
		if (object instanceof AbstractStorage)
			return ((AbstractStorage) object).storageReference;
		else if (object instanceof BigInteger)
			return new BigIntegerValue((BigInteger) object);
		else if (object instanceof Boolean)
			return new BooleanValue((Boolean) object);
		else if (object instanceof Byte)
			return new ByteValue((Byte) object);
		else if (object instanceof Character)
			return new CharValue((Character) object);
		else if (object instanceof Double)
			return new DoubleValue((Double) object);
		else if (object instanceof Float)
			return new FloatValue((Float) object);
		else if (object instanceof Integer)
			return new IntValue((Integer) object);
		else if (object instanceof Long)
			return new LongValue((Long) object);
		else if (object instanceof Short)
			return new ShortValue((Short) object);
		else if (object instanceof String)
			return new StringValue((String) object);
		else if (object instanceof Enum<?>)
			return new EnumValue(object.getClass().getName(), ((Enum<?>) object).name());
		else if (object == null)
			return NullValue.INSTANCE;
		else
			throw new IllegalArgumentException("Unserializable object " + object);
	}

	/**
	 * The size of this value, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @return the size
	 */
	BigInteger size();
}