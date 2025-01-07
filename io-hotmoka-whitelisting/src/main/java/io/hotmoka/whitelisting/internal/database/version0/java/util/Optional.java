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

package io.hotmoka.whitelisting.internal.database.version0.java.util;

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
	//java.util.stream.Stream<T> stream();
	T orElse(T other);
	T orElseGet(java.util.function.Supplier<? extends T> supplier);
	T orElseThrow();
	<X extends java.lang.Throwable> T orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws X;	
}