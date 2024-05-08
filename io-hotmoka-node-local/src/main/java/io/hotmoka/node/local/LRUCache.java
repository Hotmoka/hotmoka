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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
public final class LRUCache<K, V> {

	/**
	 * A doubly-linked-list implementation to save objects into the hashmap
	 * as key-value pari.
	 * 
	 * @author sunil
	 *
	 * @param <K>
	 * @param <V>
	 */
	private static class Node<K, V> {
		private V value;
		private final K key;
		private Node<K, V> next, prev;

		public Node(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}

	/**
	 * The maximum number of elements that can be cached, should be set during instantiation time.
	 */
	private final int maxCapacity;

	/**
	 * Use {@linkplain ConcurrentHashMap} here to maintain the cache of objects.
	 * Also this offers thread safe access of the cache.
	 */
	private final Map<K, Node<K, V>> map;

	/**
	 * A key-value representation of the cache object identified by a cache key.
	 * This is actually a doubly-linked list which maintains the most recently accessed
	 * objects (read/write) at the tail-end and the least read objects at the head. 
	 */
	private Node<K, V> head, tail;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public LRUCache(int maxCapacity) {
		this(16, maxCapacity);
	}

	public LRUCache(int initialCapacity, int maxCapacity) {
		this.maxCapacity = maxCapacity;
		this.map = new HashMap<>(Math.min(initialCapacity, maxCapacity));
	}

	public LRUCache(LRUCache<K,V> toClone) {
		toClone.lock.readLock().lock();
		try {
			this.maxCapacity = toClone.maxCapacity;
			this.map = new HashMap<>(toClone.map.size());

			for (var entry: toClone.map.entrySet())
				put(entry.getKey(), entry.getValue().value); // TODO: is it possible to inherit the LRU information in the copy?
		}
		finally {
			toClone.lock.readLock().unlock();
		}
	}

	public int size() {
		lock.readLock().lock();
		try {
			return map.size();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Removes a node from the head position doubly-linked list.
	 *
	 * @param node the node to remove
	 */
	private void removeNode(Node<K, V> node) {
		if (node.prev != null)
			node.prev.next = node.next;
		else
			head = node.next;

		if (node.next != null)
			node.next.prev = node.prev;
		else
			tail = node.prev;
	}
	/**
	 * Offers a node to the tail-end of the doubly-linked list because
	 * it was recently read or written.
	 *
	 * @param node the node offered
	 */
	private void offerNode(Node<K, V> node) {
		if (head == null)
			head = tail = node;
		else {
			tail.next = node;
			node.prev = tail;
			node.next = null;
			tail = node;
		}
	}

	/**
	 * Adds a new object to the cache. If the cache size has reached it's capacity,
	 * then the least recently accessed object will be evicted.
	 *
	 * @param key the key to bind
	 * @param value the value to bind to the {@code key}
	 */
	public void put(K key, V value) {
		lock.writeLock().lock();
		try {
			if (map.containsKey(key)) {
				Node<K, V> node = map.get(key);
				node.value = value;
				removeNode(node);
				offerNode(node);
			}
			else {
				if (map.size() == maxCapacity) {
					map.remove(head.key);
					removeNode(head);
				}

				Node<K, V> node = new Node<>(key, value);
				offerNode(node);
				map.put(key, node);
			}
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Fetches an object from the cache (could be null if no such mapping exists).
	 * If the object is found in the cache, then it will be moved to the tail-end of the
	 * doubly-linked list to indicate that it was recently accessed.
	 *
	 * @param key the key to access
	 * @return the value bound to the {@code key}
	 */
	public V get(K key) {
		lock.readLock().lock();

		try {
			Node<K, V> node = map.get(key);
			if (node == null)
				return null;

			removeNode(node);
			offerNode(node);

			return node.value;
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Clears this cache.
	 */
	public synchronized void clear() {
		lock.writeLock().lock();
		try {
			map.clear();
			head = tail = null;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

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
	public <E extends Exception> V computeIfAbsent(K key, ValueSupplier<K,V,E> supplier) throws E {
		V old = get(key);
		if (old == null) {
			V _new = supplier.supply(key);
			if (_new != null)
				put(key, _new);

			return _new;
		}
		else
			return old;
	}

	/**
	 * Adds a new object to the cache, if its key was unbound.
	 * In that case, it calls a supplier to provide the new object to add.
	 * 
	 * @param key the key of the cached value
	 * @param supplier the supplier that produces the value to put in cache
	 * @return the current (old or computed) value in cache for {@code key} at the end of the method
	 */
	public V computeIfAbsentNoException(K key, Function<K,V> supplier) {
		V old = get(key);
		if (old == null) {
			V _new = supplier.apply(key);
			if (_new != null)
				put(key, _new);

			return _new;
		}
		else
			return old;
	}

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
	public Optional<V> computeIfAbsentOptional(K key, Function<K, Optional<V>> supplier) {
		V old = get(key);
		if (old == null) {
			Optional<V> _new = supplier.apply(key);
			_new.ifPresent(v -> put(key, v));

			return _new;
		}
		else
			return Optional.of(old);
	}
}