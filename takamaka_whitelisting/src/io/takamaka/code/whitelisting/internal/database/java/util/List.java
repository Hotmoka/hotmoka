package io.takamaka.code.whitelisting.internal.database.java.util;

import io.takamaka.code.whitelisting.MustRedefineHashCode;

public interface List<E> {
	E get(int index);
	E remove(int index);
	boolean remove(java.lang.Object o);
	boolean contains(java.lang.Object o);
	void sort(java.util.Comparator<? super E> c);
	E set(int index, @MustRedefineHashCode E element);
	void add(int index, @MustRedefineHashCode E element);
	int indexOf(java.lang.Object o);
	int lastIndexOf(java.lang.Object o);
	java.util.ListIterator<E> listIterator();
	java.util.ListIterator<E> listIterator(int index);
	java.util.List<E> subList(int fromIndex, int toIndex);
	java.util.Spliterator<E> spliterator();
	static <E> java.util.List<E> of() { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8, @MustRedefineHashCode E e9) { return null; }
	static <E> java.util.List<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8, @MustRedefineHashCode E e9, @MustRedefineHashCode E e10) { return null; }
	static <E> java.util.List<E> copyOf(java.util.Collection<? extends E> coll) { return null; }
}