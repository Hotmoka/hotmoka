package io.takamaka.code.whitelisting.database.java.util;

public interface Iterator<E> {
	boolean hasNext();
	E next();
	void remove();
}