package io.takamaka.code.whitelisting.internal.database.version0.java.util;

import io.takamaka.code.whitelisting.HasDeterministicTerminatingToString;

public interface Objects {
	<T> T requireNonNull(T obj);
	<T> T requireNonNull(T obj, java.lang.String message);
	boolean isNull(java.lang.Object obj);
	boolean nonNull(java.lang.Object obj);
	<T> T requireNonNullElse(T obj, T defaultObj);
	java.lang.String toString(@HasDeterministicTerminatingToString java.lang.Object o);
}