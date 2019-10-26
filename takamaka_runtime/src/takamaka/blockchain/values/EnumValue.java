package takamaka.blockchain.values;

import java.math.BigInteger;

import io.takamaka.code.annotations.Immutable;
import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.DeserializationError;
import takamaka.blockchain.GasCosts;

/**
 * An element of an enumeration stored in blockchain.
 */
@Immutable
public final class EnumValue implements StorageValue {

	private static final long serialVersionUID = -3771826841516937906L;

	/**
	 * The name of the class of the enumeration.
	 */
	public final String enumClassName;

	/**
	 * The name of the enumeration element.
	 */
	public final String name;

	/**
	 * Builds an element of an enumeration.
	 * 
	 * @param enumClassName the class of the enumeration
	 * @param name the name of the enumeration element
	 */
	public EnumValue(String enumClassName, String name) {
		this.enumClassName = enumClassName;
		this.name = name;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Enum<?> deserialize(AbstractBlockchain blockchain) {
		try {
			return Enum.valueOf((Class<? extends Enum>) blockchain.loadClass(enumClassName), name);
		}
		catch (ClassNotFoundException e) {
			throw new DeserializationError(e);
		}
	}

	@Override
	public String toString() {
		return String.format("%s.%s", enumClassName, name);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EnumValue && ((EnumValue) other).name.equals(name)
			&& ((EnumValue) other).enumClassName.equals(enumClassName);
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ enumClassName.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		diff = enumClassName.compareTo(((EnumValue) other).enumClassName);
		if (diff != 0)
			return diff;
		else
			return name.compareTo(((EnumValue) other).name);
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.storageCostOf(enumClassName)).add(GasCosts.storageCostOf(name));
	}
}