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

package io.takamaka.code.whitelisting.internal.database.version0.java.util.stream;

public interface Stream<T> {
	<R> java.util.stream.Stream<R> of(R t);
	<R> java.util.stream.Stream<R> of(R[] values);
	<R> java.util.stream.Stream<R> ofNullable(R t);
	<R> java.util.stream.Stream<R> map(java.util.function.Function<? super T, ? extends R> mapper);
	java.lang.Object[] toArray();
	<A> A[] toArray(java.util.function.IntFunction<A[]> generator);
	java.util.stream.IntStream mapToInt(java.util.function.ToIntFunction<? super T> mapper);
	void forEachOrdered(java.util.function.Consumer<? super T> action);
	<R, A> R collect(java.util.stream.Collector<? super T, A, R> collector);
	boolean noneMatch(java.util.function.Predicate<? super T> predicate);
	boolean anyMatch(java.util.function.Predicate<? super T> predicate);
	boolean allMatch(java.util.function.Predicate<? super T> predicate);
	java.util.stream.Stream<T> filter(java.util.function.Predicate<? super T> predicate);
	java.util.stream.Stream<T> skip(long n);
	java.util.stream.Stream<T> limit(long maxSize);
	long count();
	java.util.Optional<T> min(java.util.Comparator<? super T> comparator);
	java.util.Optional<T> max(java.util.Comparator<? super T> comparator);
	T reduce(T unit, java.util.function.BinaryOperator<T> accumulator);
	java.util.Optional<T> reduce(java.util.function.BinaryOperator<T> accumulator);
	<U> U reduce(U unit, java.util.function.BiFunction<U, ? super T, U> combiner, java.util.function.BinaryOperator<U> accumulator);
	<U> java.util.stream.Stream<U> iterate(U seed, java.util.function.UnaryOperator<U> f);
}