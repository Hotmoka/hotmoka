/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.local;

import java.util.Optional;
import java.util.function.Function;

/**
 * A class to cache objects based on LRU (Least Recently Used) cache eviction strategy,
 * wherein if the cache size has reached the maximum allocated capacity, the
 * least recently used objects in the cache will be evicted.
 * 
 * @author sunil
 *
 * @param <K>
 * @param <V>
 *
 * <p>
 *        Date: 14/Nov/2019
 * </p>
 * 
 * Taken from https://dzone.com/articles/java-based-simple-cache-lru-eviction.
 */
public interface LRUCache<K, V> {

	int size();

	/**
	 * Adds a new object to the cache. If the cache size has reached it's capacity,
	 * then the least recently accessed object will be evicted.
	 *
	 * @param key the key to bind
	 * @param value the value to bind to the {@code key}
	 */
	void put(K key, V value);

	/**
	 * Fetches an object from the cache (could be null if no such mapping exists).
	 * If the object is found in the cache, then it will be moved to the tail-end of the
	 * doubly-linked list to indicate that it was recently accessed.
	 *
	 * @param key the key to access
	 * @return the value bound to the {@code key}
	 */
	V get(K key);

	/**
	 * Clears this cache.
	 */
	void clear();

	public interface ValueSupplier<K,V,E extends Exception> {
		V supply(K key) throws E;
	}

	/**
	 * Adds a new object to the cache, if its key was unbound.
	 * In that case, it calls a supplier to provide the new object to add.
	 * 
	 * @param key the key of the cached value
	 * @param supplier the supplier that produces the value to put in cache
	 * @return the current (old or computed) value in cache for {@code key} at the end of the method
	 */
	<E extends Exception> V computeIfAbsent(K key, ValueSupplier<K,V,E> supplier) throws E;

	/**
	 * Adds a new object to the cache, if its key was unbound.
	 * In that case, it calls a supplier to provide the new object to add.
	 * 
	 * @param key the key of the cached value
	 * @param supplier the supplier that produces the value to put in cache
	 * @return the current (old or computed) value in cache for {@code key} at the end of the method
	 */
	V computeIfAbsentNoException(K key, Function<K,V> supplier);

	/**
	 * Adds a new object to the cache, if its key was unbound.
	 * In that case, it calls a supplier to provide the new object to add.
	 * If the supplier yields an empty optional, nothing is added to the map.
	 * 
	 * @param key the key of the cached value
	 * @param supplier the supplier that produces the value to put in cache
	 * @return the current (old or computed) value in cache for {@code key} at the end of the method;
	 *         if the cache did not contain a value for the key and the supplier returns
	 *         an empty optional, then an empty optional is returned
	 */
	Optional<V> computeIfAbsentOptional(K key, Function<K, Optional<V>> supplier);
}