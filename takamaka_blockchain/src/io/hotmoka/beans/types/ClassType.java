package io.hotmoka.beans.types;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A class type that can be used for stored objects in blockchain.
 */
@Immutable
public final class ClassType implements StorageType {

	private static final long serialVersionUID = -501005311788239209L;

	/**
	 * The name of the class type.
	 */
	public final String name;

	/**
	 * A cache for {@link io.hotmoka.beans.types.ClassType#mk(String)}.
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
	 * Clears the cache used for {@link io.hotmoka.beans.types.ClassType#mk(String)}.
	 */
	public static void clearCache() {
		cache.clear();
	}

	@Override
	public String toString() {
		return name;
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
}