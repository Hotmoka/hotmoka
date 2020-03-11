package io.takamaka.code.whitelisting.internal.database.java.util;

import io.takamaka.code.whitelisting.MustRedefineHashCode;

public interface Set<E> {
	boolean containsAll(java.util.Collection<?> c);
	boolean addAll(java.util.Collection<? extends E> c);
	boolean retainAll(java.util.Collection<?> c);
	boolean removeAll(java.util.Collection<?> c);
	static <E> java.util.Set<E> of() { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8, @MustRedefineHashCode E e9) { return null; }
	static <E> java.util.Set<E> of(@MustRedefineHashCode E e1, @MustRedefineHashCode E e2, @MustRedefineHashCode E e3, @MustRedefineHashCode E e4, @MustRedefineHashCode E e5, @MustRedefineHashCode E e6, @MustRedefineHashCode E e7, @MustRedefineHashCode E e8, @MustRedefineHashCode E e9, @MustRedefineHashCode E e10) { return null; }
	static <E> java.util.Set<E> copyOf(java.util.Collection<? extends E> coll) { return null; }
}