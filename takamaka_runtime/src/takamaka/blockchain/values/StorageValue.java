package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Storage;

public interface StorageValue extends Comparable<StorageValue> {
	Object deserialize(Blockchain blockchain) throws TransactionException;

	public static StorageValue serialize(Object object) throws IllegalArgumentException {
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