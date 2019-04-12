package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.lang.Storage;

public interface StorageValue extends Comparable<StorageValue> {
	Object deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) throws TransactionException;

	/**
	 * Yields a storage value of a given type, from its string representation.
	 * It always hold that {@code from(type, value.toString()).equals(value)},
	 * if {@code value} has type {@code type}.
	 * 
	 * @param blokchain the blokchain for which the value is being looked for
	 * @param type the type of the value
	 * @param s the string representation of the value
	 * @return the value
	 * @throws IllegalArgumentException if booleans or characters cannot be converted
	 * @throws NumberFormatException if numerical values cannot be converted
	 * @throws RuntimeException if an unexpected type is provided
	 */
	static StorageValue of(AbstractBlockchain blokchain, StorageType type, String s) {
		if (s == null)
			throw new IllegalArgumentException("The string to convert cannot be null");

		if (type instanceof BasicTypes) {
			switch ((BasicTypes) type) {
			case BOOLEAN:
				if (s.equals("true"))
					return new BooleanValue(true);
				else if (s.equals("false"))
					return new BooleanValue(false);
				else
					throw new IllegalArgumentException("The string to convert is not a boolean");
			case BYTE:
				return new ByteValue(Byte.parseByte(s));
			case CHAR:
				if (s.length() != 1)
					throw new IllegalArgumentException("The string to convert is not a character");
				else
					return new CharValue(s.charAt(0));
			case DOUBLE:
				return new DoubleValue(Double.parseDouble(s));
			case FLOAT:
				return new FloatValue(Float.parseFloat(s));
			case INT:
				return new IntValue(Integer.parseInt(s));
			case LONG:
				return new LongValue(Long.parseLong(s));
			case SHORT:
				return new ShortValue(Short.parseShort(s));
			default:
				throw new RuntimeException("Unexpected basic type " + type);
			}
		}
		else if (type instanceof ClassType) {
			if (s.equals("null"))
				return NullValue.INSTANCE;
			else if (type.equals(ClassType.STRING))
				return new StringValue(s);
			else if (type.equals(ClassType.BIG_INTEGER))
				return new BigIntegerValue(new BigInteger(s, 10));
			else
				return new StorageReference(blokchain, s);
		}

		throw new RuntimeException("Unexpected type " + type);
	}

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