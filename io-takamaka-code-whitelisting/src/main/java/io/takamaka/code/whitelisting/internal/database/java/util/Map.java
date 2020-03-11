package io.takamaka.code.whitelisting.internal.database.java.util;

import io.takamaka.code.whitelisting.MustRedefineHashCode;

public interface Map<K, V> {
	int size();
	boolean isEmpty();
	V put(@MustRedefineHashCode K key, V value);
	java.util.Set<K> keySet();
	boolean containsKey(@MustRedefineHashCode java.lang.Object key);
	boolean containsValue(java.lang.Object value);
	V get(@MustRedefineHashCode java.lang.Object key);
	V remove(@MustRedefineHashCode java.lang.Object key);
	void putAll(java.util.Map<? extends K, ? extends V> m);
	void clear();
	V getOrDefault(@MustRedefineHashCode java.lang.Object key, V defaultValue);
	void forEach(java.util.function.BiConsumer<? super K, ? super V> action);
	void replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V> function);
	V putIfAbsent(@MustRedefineHashCode K key, V value);
	boolean remove(@MustRedefineHashCode java.lang.Object key, java.lang.Object value);
	boolean replace(@MustRedefineHashCode K key, V oldValue, V newValue);
	V replace(@MustRedefineHashCode K key, V value);
	V computeIfAbsent(@MustRedefineHashCode K key, java.util.function.Function<? super K, ? extends V> mappingFunction);
	V computeIfPresent(@MustRedefineHashCode K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction);
	V compute(@MustRedefineHashCode K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction);
	V merge(@MustRedefineHashCode K key, V value, java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction);
	static <K, V> java.util.Map<K, V> of() { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1) { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2) { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2, @MustRedefineHashCode K k3, V v3) { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2, @MustRedefineHashCode K k3, V v3, @MustRedefineHashCode K k4, V v4) { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2, @MustRedefineHashCode K k3, V v3, @MustRedefineHashCode K k4, V v4, @MustRedefineHashCode K k5, V v5)  {  return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2, @MustRedefineHashCode K k3, V v3, @MustRedefineHashCode K k4, V v4, @MustRedefineHashCode K k5, V v5, @MustRedefineHashCode K k6, V v6) { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2, @MustRedefineHashCode K k3, V v3, @MustRedefineHashCode K k4, V v4, @MustRedefineHashCode K k5, V v5, @MustRedefineHashCode K k6, V v6, @MustRedefineHashCode K k7, V v7) { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2, @MustRedefineHashCode K k3, V v3, @MustRedefineHashCode K k4, V v4, @MustRedefineHashCode K k5, V v5, @MustRedefineHashCode K k6, V v6, @MustRedefineHashCode K k7, V v7, @MustRedefineHashCode K k8, V v8) { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2, @MustRedefineHashCode K k3, V v3, @MustRedefineHashCode K k4, V v4, @MustRedefineHashCode K k5, V v5, @MustRedefineHashCode K k6, V v6, @MustRedefineHashCode K k7, V v7, @MustRedefineHashCode K k8, V v8, @MustRedefineHashCode K k9, V v9) { return null; }
	static <K, V> java.util.Map<K, V> of(@MustRedefineHashCode K k1, V v1, @MustRedefineHashCode K k2, V v2, @MustRedefineHashCode K k3, V v3, @MustRedefineHashCode K k4, V v4, @MustRedefineHashCode K k5, V v5, @MustRedefineHashCode K k6, V v6, @MustRedefineHashCode K k7, V v7, @MustRedefineHashCode K k8, V v8, @MustRedefineHashCode K k9, V v9, @MustRedefineHashCode K k10, V v10) { return null; }
	static <K, V> java.util.Map<K, V> ofEntries(java.util.Map.Entry<? extends K, ? extends V>[] entries) { return null; }
	static <K, V> java.util.Map.Entry<K, V> entry(@MustRedefineHashCode K k, V v) { return null; }
	static <K, V> java.util.Map<K, V> copyOf(java.util.Map<? extends K, ? extends V> map) { return null; }
}