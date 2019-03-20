package takamaka.util;

import java.util.function.Function;

import takamaka.lang.Storage;

public class StorageMap<K,E> extends Storage {
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