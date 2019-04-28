package takamaka.util;

import java.util.function.Function;

import takamaka.lang.Storage;

/**
 * A map between storage objects.
 *
 * @param <K> the type of the keys
 * @param <E> the type of the elements
 */
public class StorageMap<K, E> extends Storage {
	
	//TODO: not allowed in storage class
	private final Function<K,E> provider;

	public StorageMap() {
		this(k -> null);
	}

	public StorageMap(Function<K,E> provider) {
		this.provider = provider;
	}

	public void put(K key, E element) {}
	public E get(K key) { return null; }
}