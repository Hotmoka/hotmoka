package io.takamaka.code.whitelisting.internal.database.java.util;

public abstract class ArrayList<E> {
	public ArrayList() {}
	public ArrayList(int size) {}
	public ArrayList(java.util.Collection<? extends E> c) {}
	public abstract void trimToSize();
	public abstract void ensureCapacity(int minCapacity);
}