package io.hotmoka.beans.values;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;

/**
 * An element of an enumeration stored in blockchain.
 */
@Immutable
public final class EnumValue extends StorageValue {
	static final byte SELECTOR = 12;

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
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(enumClassName)).add(gasCostModel.storageCostOf(name));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		context.oos.writeUTF(enumClassName);
		context.oos.writeUTF(name);
	}
}