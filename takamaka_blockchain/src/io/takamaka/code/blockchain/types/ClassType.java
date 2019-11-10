package io.takamaka.code.blockchain.types;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.GasCosts;
import io.takamaka.code.blockchain.annotations.Immutable;

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
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = new ClassType("io.takamaka.code.lang.ExternallyOwnedAccount");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.TestExternallyOwnedAccount}.
	 */
	public final static ClassType TEOA = new ClassType("io.takamaka.code.lang.TestExternallyOwnedAccount");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static ClassType CONTRACT = new ClassType("io.takamaka.code.lang.Contract");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static ClassType STORAGE = new ClassType("io.takamaka.code.lang.Storage");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static ClassType TAKAMAKA = new ClassType("io.takamaka.code.lang.Takamaka");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static ClassType EVENT = new ClassType("io.takamaka.code.lang.Event");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static ClassType PAYABLE_CONTRACT = new ClassType("io.takamaka.code.lang.PayableContract");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Entry}.
	 */
	public final static ClassType ENTRY = new ClassType("io.takamaka.code.lang.Entry");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static ClassType VIEW = new ClassType("io.takamaka.code.lang.View");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static ClassType PAYABLE = new ClassType("io.takamaka.code.lang.Payable");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static ClassType THROWS_EXCEPTIONS = new ClassType("io.takamaka.code.lang.ThrowsExceptions");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32}.
	 */
	public final static ClassType BYTES32 = new ClassType("io.takamaka.code.util.Bytes32");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageList}.
	 */
	public final static ClassType STORAGE_LIST = new ClassType("io.takamaka.code.util.StorageList");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static ClassType STORAGE_MAP = new ClassType("io.takamaka.code.util.StorageMap");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.RequirementViolationException}.
	 */
	public final static ClassType REQUIREMENT_VIOLATION_EXCEPTION = new ClassType("io.takamaka.code.lang.RequirementViolationException");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.InsufficientFundsError}.
	 */
	public final static ClassType INSUFFICIENT_FUNDS_ERROR = new ClassType("io.takamaka.code.lang.InsufficientFundsError");

	/**
	 * The name of the class type.
	 */
	public final String name;

	/**
	 * A cache for {@link io.takamaka.code.blockchain.types.ClassType#mk(String)}.
	 */
	private static Map<String, ClassType> cache = new ConcurrentHashMap<>();

	/**
	 * Builds a class type that can be used for storage objects in blockchain.
	 * 
	 * @param name the name of the class
	 */
	public ClassType(String name) {
		this.name = name;
	}

	/**
	 * Yields a class type that can be used for storage objects in blockchain.
	 * This corresponds to the constructor, but adds a caching layer.
	 * 
	 * @param name the name of the class
	 */
	public static ClassType mk(String name) {
		return cache.computeIfAbsent(name, ClassType::new);
	}

	/**
	 * Clears the cache used for {@link io.takamaka.code.blockchain.types.ClassType#mk(String)}.
	 */
	public static void clearCache() {
		cache.clear();
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
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.storageCostOf(name));
	}
}