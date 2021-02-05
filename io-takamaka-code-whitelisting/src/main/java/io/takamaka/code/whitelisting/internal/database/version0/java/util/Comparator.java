package io.takamaka.code.whitelisting.internal.database.version0.java.util;

public interface Comparator<T> {
	<D> java.util.Comparator<D> comparingInt(java.util.function.ToIntFunction<? super D> keyExtractor);
	<D> java.util.Comparator<D> comparingLong(java.util.function.ToLongFunction<? super D> keyExtractor);
	<D> java.util.Comparator<D> comparingDouble(java.util.function.ToDoubleFunction<? super D> keyExtractor);
}