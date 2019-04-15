package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.lang.Storage;

/**
 * A value that can be stored in the blockchain, passed as argument of an antry
 * or returned from an entry.
 */
public interface StorageValue extends Comparable<StorageValue> {

	/**
	 * Yields the deserialization of this value.
	 * 
	 * @param classLoader the class loader that must be used for the definition of the storage classes
	 * @param blockchain the blockchain for which the deserialization is performed
	 * @return the deserialization
	 */
	Object deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain);

	static StorageValue serialize(Object object) throws IllegalArgumentException {
		if (object instanceof Storage)
			return ((Storage) object).storageReference;
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
		else if (object == null)
			return NullValue.INSTANCE;
		else
			throw new IllegalArgumentException("Unserializable object " + object);
	}
}