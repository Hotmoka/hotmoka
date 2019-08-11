package takamaka.whitelisted.java.util;

public interface Collection<E> {
	int size();
	boolean isEmpty();
	java.util.Iterator<E> iterator();
	java.lang.Object[] toArray();
	<T> T[] toArray(T[] a);
	<T> T[] toArray(java.util.function.IntFunction<T[]> generator);
	boolean removeIf(java.util.function.Predicate<? super E> filter);
	void clear();
	java.util.stream.Stream<E> stream();
}