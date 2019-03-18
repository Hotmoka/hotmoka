package takamaka.blockchain.types;

import takamaka.blockchain.BlockchainClassLoader;
import takamaka.lang.Immutable;

@Immutable
public final class ClassType implements StorageType {

	/**
	 * The frequently used class type for {@code Object}.
	 */
	public final static ClassType OBJECT = new ClassType("java.lang.Object");

	/**
	 * The frequently used class type for strings.
	 */
	public final static ClassType STRING = new ClassType("java.lang.String");

	/**
	 * The frequently used class type for big integers.
	 */
	public final static ClassType BIG_INTEGER = new ClassType("java.math.BigInteger");

	public final String name;

	public ClassType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public Class<?> toClass(BlockchainClassLoader classLoader) throws ClassNotFoundException {
		return classLoader.loadClass(name);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassType && ((ClassType) other).name.equals(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public int compareAgainst(StorageType other) {
		if (other instanceof BasicTypes)
			return 1;
		else
			return name.compareTo(((ClassType) other).name); // other instanceof ClassType
	}

	@Override
	public boolean isLazilyLoaded() {
		return !equals(STRING) && !equals(BIG_INTEGER);
	}
}