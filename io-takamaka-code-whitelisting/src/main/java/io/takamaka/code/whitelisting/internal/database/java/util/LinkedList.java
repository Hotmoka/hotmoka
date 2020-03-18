package io.takamaka.code.whitelisting.internal.database.java.util;

import io.takamaka.code.whitelisting.HasDeterministicTerminatingEqualsAndHashCode;

public abstract class LinkedList<E> {
	public LinkedList() {}
	public LinkedList(java.util.Collection<? extends E> c) {}
	public abstract E getFirst();
	public abstract E getLast();
	public abstract E removeFirst();
	public abstract E removeLast();
	public abstract void addFirst(@HasDeterministicTerminatingEqualsAndHashCode E e);
	public abstract void addLast(@HasDeterministicTerminatingEqualsAndHashCode E e);
}