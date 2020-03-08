package io.takamaka.code.whitelisting.internal.database.java.util;

public interface Iterator<E> {
	boolean hasNext();
	E next();
	void remove();
}