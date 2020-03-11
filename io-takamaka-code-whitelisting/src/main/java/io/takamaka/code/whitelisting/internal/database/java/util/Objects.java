package io.takamaka.code.whitelisting.internal.database.java.util;

import io.takamaka.code.whitelisting.MustRedefineHashCodeOrToString;

public interface Objects {
	<T> T requireNonNull(T obj);
	<T> T requireNonNull(T obj, java.lang.String message);
	boolean isNull(java.lang.Object obj);
	boolean nonNull(java.lang.Object obj);
	<T> T requireNonNullElse(T obj, T defaultObj);
	java.lang.String toString(@MustRedefineHashCodeOrToString java.lang.Object o);
}