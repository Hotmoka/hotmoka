package io.takamaka.code.whitelisting.internal.database.version0.java.util.stream;

public interface IntStream {
	java.util.stream.IntStream iterate(int seed, java.util.function.IntUnaryOperator next);
	java.util.stream.IntStream iterate(int seed, java.util.function.IntPredicate condition, java.util.function.IntUnaryOperator next);
	java.util.stream.IntStream range(int startInclusive, int endExclusive);
	java.util.stream.IntStream rangeClosed(int startInclusive, int endInclusive);
	java.util.stream.IntStream of(int t);
	java.util.stream.IntStream of(int... values);
	<U> java.util.stream.Stream<U> mapToObj(java.util.function.IntFunction<? extends U> mapper);
	int sum();
	void forEachOrdered(java.util.function.IntConsumer action);
	boolean allMatch(java.util.function.IntPredicate predicate);
}