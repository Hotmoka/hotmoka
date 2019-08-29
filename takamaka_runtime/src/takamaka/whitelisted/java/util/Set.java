package takamaka.whitelisted.java.util;

import takamaka.lang.MustRedefineHashCode;

public interface Set<E> {
	int size();
	boolean isEmpty();
	boolean contains(@MustRedefineHashCode java.lang.Object o);
	java.util.Iterator<E> iterator();
	java.lang.Object[] toArray();
	<T> T[] toArray(T[] a);
	boolean add(@MustRedefineHashCode E e);
	boolean remove(@MustRedefineHashCode java.lang.Object o);
	boolean containsAll(java.util.Collection<?> c);
	boolean addAll(java.util.Collection<? extends E> c);
	boolean retainAll(java.util.Collection<?> c);
	boolean removeAll(java.util.Collection<?> c);
	void clear();
	static <E> Set<E> of() { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8, @MustRedefineHashCode E e9) { return null; }
	static <E> Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8, @MustRedefineHashCode E e9, @MustRedefineHashCode E e10) { return null; }
	static <E> Set<E> copyOf(java.util.Collection<? extends E> coll) { return null; }
}