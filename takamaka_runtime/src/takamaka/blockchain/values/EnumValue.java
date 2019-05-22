package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.DeserializationError;
import takamaka.blockchain.types.ClassType;
import takamaka.lang.Immutable;

/**
 * An element of an enumeration stored in blockchain.
 */
@Immutable
public final class EnumValue implements StorageValue {

	private static final long serialVersionUID = -3771826841516937906L;

	/**
	 * The class of the enumeration.
	 */
	public final ClassType clazz;

	/**
	 * The name of the enumeration element.
	 */
	public final String name;

	/**
	 * Builds an element of an enumeration.
	 * 
	 * @param clazz the class of the enumeration
	 * @param name the name of the enumeration element
	 */
	public EnumValue(ClassType clazz, String name) {
		this.clazz = clazz;
		this.name = name;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Enum<?> deserialize(AbstractBlockchain blockchain) {
		try {
			Class<?> enumClass = (Class<?>) clazz.toClass(blockchain);
			return Enum.valueOf((Class<? extends Enum>) enumClass, name);
		}
		catch (ClassNotFoundException e) {
			throw new DeserializationError(e);
		}
	}

	@Override
	public String toString() {
		return String.format("%s#%s", clazz, name);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EnumValue && ((EnumValue) other).name == name && ((EnumValue) other).clazz.equals(clazz);
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ clazz.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		diff = clazz.name.compareTo(((EnumValue) other).clazz.name);
		if (diff != 0)
			return diff;
		else
			return name.compareTo(((EnumValue) other).name);
	}
}