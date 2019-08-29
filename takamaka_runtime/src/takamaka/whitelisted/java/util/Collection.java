package takamaka.whitelisted.java.util;

import takamaka.lang.MustBeOrdered;
import takamaka.lang.MustRedefineHashCode;

public interface Collection<E> {
	int size();
	boolean isEmpty();
	boolean contains(@MustRedefineHashCode java.lang.Object o);
	java.util.Iterator<E> iterator();
	java.lang.Object[] toArray();
	<T> T[] toArray(T[] a);
	<T> T[] toArray(java.util.function.IntFunction<T[]> generator);
	boolean add(@MustRedefineHashCode E e);
	boolean remove(@MustRedefineHashCode java.lang.Object o);
	boolean containsAll(java.util.Collection<?> c);
	boolean addAll(java.util.Collection<? extends E> c);
	boolean removeAll(java.util.Collection<?> c);
	boolean removeIf(java.util.function.Predicate<? super E> filter);
	boolean retainAll(java.util.Collection<?> c);
	void clear();
	@MustBeOrdered java.util.stream.Stream<E> stream();
}