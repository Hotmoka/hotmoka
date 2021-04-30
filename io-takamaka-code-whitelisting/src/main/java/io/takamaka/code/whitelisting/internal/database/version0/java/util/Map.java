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

package io.takamaka.code.whitelisting.internal.database.version0.java.util;

import io.takamaka.code.whitelisting.HasDeterministicTerminatingEqualsAndHashCode;

public interface Map<K, V> {
	int size();
	boolean isEmpty();
	V put(@HasDeterministicTerminatingEqualsAndHashCode K key, V value);
	java.util.Set<K> keySet();
	boolean containsKey(@HasDeterministicTerminatingEqualsAndHashCode java.lang.Object key);
	boolean containsValue(java.lang.Object value);
	V get(@HasDeterministicTerminatingEqualsAndHashCode java.lang.Object key);
	V remove(@HasDeterministicTerminatingEqualsAndHashCode java.lang.Object key);
	void putAll(java.util.Map<? extends K, ? extends V> m);
	void clear();
	V getOrDefault(@HasDeterministicTerminatingEqualsAndHashCode java.lang.Object key, V defaultValue);
	void forEach(java.util.function.BiConsumer<? super K, ? super V> action);
	void replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V> function);
	V putIfAbsent(@HasDeterministicTerminatingEqualsAndHashCode K key, V value);
	boolean remove(@HasDeterministicTerminatingEqualsAndHashCode java.lang.Object key, java.lang.Object value);
	boolean replace(@HasDeterministicTerminatingEqualsAndHashCode K key, V oldValue, V newValue);
	V replace(@HasDeterministicTerminatingEqualsAndHashCode K key, V value);
	V computeIfAbsent(@HasDeterministicTerminatingEqualsAndHashCode K key, java.util.function.Function<? super K, ? extends V> mappingFunction);
	V computeIfPresent(@HasDeterministicTerminatingEqualsAndHashCode K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction);
	V compute(@HasDeterministicTerminatingEqualsAndHashCode K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction);
	V merge(@HasDeterministicTerminatingEqualsAndHashCode K key, V value, java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction);
	static <K, V> java.util.Map<K, V> of() { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1) { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2) { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2, @HasDeterministicTerminatingEqualsAndHashCode K k3, V v3) { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2, @HasDeterministicTerminatingEqualsAndHashCode K k3, V v3, @HasDeterministicTerminatingEqualsAndHashCode K k4, V v4) { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2, @HasDeterministicTerminatingEqualsAndHashCode K k3, V v3, @HasDeterministicTerminatingEqualsAndHashCode K k4, V v4, @HasDeterministicTerminatingEqualsAndHashCode K k5, V v5)  {  return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2, @HasDeterministicTerminatingEqualsAndHashCode K k3, V v3, @HasDeterministicTerminatingEqualsAndHashCode K k4, V v4, @HasDeterministicTerminatingEqualsAndHashCode K k5, V v5, @HasDeterministicTerminatingEqualsAndHashCode K k6, V v6) { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2, @HasDeterministicTerminatingEqualsAndHashCode K k3, V v3, @HasDeterministicTerminatingEqualsAndHashCode K k4, V v4, @HasDeterministicTerminatingEqualsAndHashCode K k5, V v5, @HasDeterministicTerminatingEqualsAndHashCode K k6, V v6, @HasDeterministicTerminatingEqualsAndHashCode K k7, V v7) { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2, @HasDeterministicTerminatingEqualsAndHashCode K k3, V v3, @HasDeterministicTerminatingEqualsAndHashCode K k4, V v4, @HasDeterministicTerminatingEqualsAndHashCode K k5, V v5, @HasDeterministicTerminatingEqualsAndHashCode K k6, V v6, @HasDeterministicTerminatingEqualsAndHashCode K k7, V v7, @HasDeterministicTerminatingEqualsAndHashCode K k8, V v8) { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2, @HasDeterministicTerminatingEqualsAndHashCode K k3, V v3, @HasDeterministicTerminatingEqualsAndHashCode K k4, V v4, @HasDeterministicTerminatingEqualsAndHashCode K k5, V v5, @HasDeterministicTerminatingEqualsAndHashCode K k6, V v6, @HasDeterministicTerminatingEqualsAndHashCode K k7, V v7, @HasDeterministicTerminatingEqualsAndHashCode K k8, V v8, @HasDeterministicTerminatingEqualsAndHashCode K k9, V v9) { return null; }
	static <K, V> java.util.Map<K, V> of(@HasDeterministicTerminatingEqualsAndHashCode K k1, V v1, @HasDeterministicTerminatingEqualsAndHashCode K k2, V v2, @HasDeterministicTerminatingEqualsAndHashCode K k3, V v3, @HasDeterministicTerminatingEqualsAndHashCode K k4, V v4, @HasDeterministicTerminatingEqualsAndHashCode K k5, V v5, @HasDeterministicTerminatingEqualsAndHashCode K k6, V v6, @HasDeterministicTerminatingEqualsAndHashCode K k7, V v7, @HasDeterministicTerminatingEqualsAndHashCode K k8, V v8, @HasDeterministicTerminatingEqualsAndHashCode K k9, V v9, @HasDeterministicTerminatingEqualsAndHashCode K k10, V v10) { return null; }
	static <K, V> java.util.Map<K, V> ofEntries(java.util.Map.Entry<? extends K, ? extends V>[] entries) { return null; }
	static <K, V> java.util.Map.Entry<K, V> entry(@HasDeterministicTerminatingEqualsAndHashCode K k, V v) { return null; }
	static <K, V> java.util.Map<K, V> copyOf(java.util.Map<? extends K, ? extends V> map) { return null; }
}