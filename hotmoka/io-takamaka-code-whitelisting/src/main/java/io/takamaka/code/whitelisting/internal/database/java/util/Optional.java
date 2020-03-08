package io.takamaka.code.whitelisting.internal.database.java.util;

public interface Optional<T> {
	<D> java.util.Optional<D> empty();
	<D> java.util.Optional<D> of(D value);
	<D> java.util.Optional<D> ofNullable(D value);
	T get();
	boolean isPresent();
	boolean isEmpty();
	void ifPresent(java.util.function.Consumer<? super T> action);
	void ifPresentOrElse(java.util.function.Consumer<? super T> action, java.lang.Runnable emptyAction);
	java.util.Optional<T> filter(java.util.function.Predicate<? super T> predicate);
	<U> java.util.Optional<U> map(java.util.function.Function<? super T, ? extends U> mapper);
	<U> java.util.Optional<U> flatMap(java.util.function.Function<? super T, ? extends java.util.Optional<? extends U>> mapper);
	java.util.Optional<T> or(java.util.function.Supplier<? extends java.util.Optional<? extends T>> supplier);
	java.util.stream.Stream<T> stream();
	T orElse(T other);
	T orElseGet(java.util.function.Supplier<? extends T> supplier);
	T orElseThrow();
	<X extends java.lang.Throwable> T orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws X;	
}