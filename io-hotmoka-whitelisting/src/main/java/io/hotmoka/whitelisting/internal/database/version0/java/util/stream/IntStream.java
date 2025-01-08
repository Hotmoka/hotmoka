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

package io.hotmoka.whitelisting.internal.database.version0.java.util.stream;

public interface IntStream {
	java.util.stream.IntStream empty();
	java.util.stream.IntStream iterate(int seed, java.util.function.IntUnaryOperator next);
	java.util.stream.IntStream iterate(int seed, java.util.function.IntPredicate condition, java.util.function.IntUnaryOperator next);
	java.util.stream.IntStream range(int startInclusive, int endExclusive);
	java.util.stream.IntStream rangeClosed(int startInclusive, int endInclusive);
	java.util.stream.IntStream generate(java.util.function.IntSupplier supplier);
	java.util.stream.IntStream of(int t);
	java.util.stream.IntStream of(int... values);
	java.util.stream.IntStream takeWhile(java.util.function.IntPredicate predicate);
	java.util.stream.IntStream map(java.util.function.IntUnaryOperator mapper);
	java.util.stream.IntStream filter(java.util.function.IntPredicate predicate);
	<U> java.util.stream.Stream<U> mapToObj(java.util.function.IntFunction<? extends U> mapper);
	void forEachOrdered(java.util.function.IntConsumer action);
	boolean allMatch(java.util.function.IntPredicate predicate);
}