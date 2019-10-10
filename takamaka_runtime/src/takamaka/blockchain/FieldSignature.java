package takamaka.blockchain;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.lang.Immutable;

/**
 * The signature of a field of a class.
 */
@Immutable
public final class FieldSignature implements Serializable, Comparable<FieldSignature> {

	private static final long serialVersionUID = -233403674197930650L;

	/**
	 * The field that holds the balance in externally owned accounts.
	 */
	public final static FieldSignature BALANCE_FIELD = new FieldSignature(ClassType.CONTRACT, "balance", ClassType.BIG_INTEGER);

	/**
	 * The class of the field.
	 */
	public final ClassType definingClass;

	/**
	 * The name of the field.
	 */
	public final String name;

	/**
	 * The type of the field.
	 */
	public final StorageType type;

	/**
	 * A cache for {@link takamaka.blockchain.FieldSignature#mk(String, String, StorageType)}.
	 */
	private static Map<FieldSignature, FieldSignature> cache = new ConcurrentHashMap<>();

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	private FieldSignature(ClassType definingClass, String name, StorageType type) {
		this.definingClass = definingClass;
		this.name = name.intern(); // to reduce the size at serialization time
		this.type = type;
	}

	/**
	 * Yields a field signature.
	 * This corresponds to the constructor, but adds a caching layer.
	 * 
	 * @param definingClass the name of the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public static FieldSignature mk(String definingClass, String name, StorageType type) {
		FieldSignature sig = new FieldSignature(ClassType.mk(definingClass), name, type);
		return cache.computeIfAbsent(sig, __ -> sig);
	}

	/**
	 * Clears the cache used for {@link takamaka.blockchain.FieldSignature#mk(String, String, StorageType)}.
	 */
	public static void clearCache() {
		cache.clear();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldSignature && ((FieldSignature) other).definingClass.equals(definingClass)
			&& ((FieldSignature) other).name.equals(name) && ((FieldSignature) other).type.equals(type);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ name.hashCode() ^ type.hashCode();
	}

	@Override
	public String toString() {
		return definingClass + "." + name + ":" + type;
	}

	@Override
	public int compareTo(FieldSignature other) {
		int diff = definingClass.compareAgainst(other.definingClass);
		if (diff != 0)
			return diff;

		diff = name.compareTo(other.name);
		if (diff != 0)
			return diff;
		else
			return type.compareAgainst(other.type);
	}

	/**
	 * Yields the size of this field, in terms of storage consumed if this field is
	 * stored in blockchain.
	 * 
	 * @return the size
	 */
	public int size() {
		return 3 + name.length() / 4 + definingClass.name.length() / 4 + type.size();
	}
}