package takamaka.blockchain.types;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.lang.Immutable;

/**
 * A class type that can be used for stored objects in blockchain.
 */
@Immutable
public final class ClassType implements StorageType {

	private static final long serialVersionUID = -501005311788239209L;

	/**
	 * The frequently used class type for {@link java.lang.Object}.
	 */
	public final static ClassType OBJECT = new ClassType("java.lang.Object");

	/**
	 * The frequently used class type for {@link java.lang.String}.
	 */
	public final static ClassType STRING = new ClassType("java.lang.String");

	/**
	 * The frequently used class type for {@link java.math.BigInteger}.
	 */
	public final static ClassType BIG_INTEGER = new ClassType("java.math.BigInteger");

	/**
	 * The frequently used class type for {@link takamaka.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = new ClassType("takamaka.lang.ExternallyOwnedAccount");

	/**
	 * The frequently used class type for {@link takamaka.lang.Contract}.
	 */
	public final static ClassType CONTRACT = new ClassType("takamaka.lang.Contract");

	/**
	 * The name of the class type.
	 */
	public final String name;

	/**
	 * Builds a class type that can be used for storage objects in blockchain.
	 * 
	 * @param name the name of the class
	 */
	public ClassType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public Class<?> toClass(AbstractBlockchain blockchain) throws ClassNotFoundException {
		return blockchain.loadClass(name);
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
	public boolean isLazy() {
		return !equals(STRING) && !equals(BIG_INTEGER);
	}
}