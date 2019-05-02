package takamaka.util;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import takamaka.lang.Storage;

/**
 * A map between storage objects.
 *
 * @param <K> the type of the keys
 * @param <E> the type of the elements
 */
public class StorageMap<K,E> extends Storage {

	public StorageMap() {}

	public E get(K key) { return null; }
	public E get(K key, E _default) { return null; }
	public E get(K key, Supplier<E> _default) { return null; }
	public void put(K key, E element) {}

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code update.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code update.apply(null)},
	 * which might well lead to a run-time exception.
	 *
	 * @param key the key whose value must be replaced
	 * @param how the replacement function
	 */
	public void update(K key, UnaryOperator<E> how) {}

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code update.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code update.apply(_default)}.
	 *
	 * @param key the key whose value must be replaced
	 * @param _default the default value
	 * @param how the replacement function
	 */
	public void update(K key, E _default, UnaryOperator<E> how) {}

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code update.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code update.apply(_default.get())}.
	 *
	 * @param key the key whose value must be replaced
	 * @param _default the provider of the default value
	 * @param how the replacement function
	 */
	public void update(K key, Supplier<E> _default, UnaryOperator<E> how) {}
}