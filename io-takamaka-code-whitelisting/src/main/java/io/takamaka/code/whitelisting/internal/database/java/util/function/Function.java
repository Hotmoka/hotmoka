package io.takamaka.code.whitelisting.internal.database.java.util.function;

public interface Function<T, R> {
	R apply(T t);
	<V> java.util.function.Function<V, R> compose(java.util.function.Function<? super V, ? extends T> before);
	<V> java.util.function.Function<T, V> andThen(java.util.function.Function<? super R, ? extends V> after);
	<X> java.util.function.Function<X, X> identity();
}