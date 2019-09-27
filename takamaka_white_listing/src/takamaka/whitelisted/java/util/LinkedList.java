package takamaka.whitelisted.java.util;

import takamaka.whitelisted.MustRedefineHashCode;

public abstract class LinkedList<E> {
	public LinkedList() {}
	public LinkedList(java.util.Collection<? extends E> c) {}
	public abstract E getFirst();
	public abstract E getLast();
	public abstract E removeFirst();
	public abstract E removeLast();
	public abstract void addFirst(@MustRedefineHashCode E e);
	public abstract void addLast(@MustRedefineHashCode E e);
}