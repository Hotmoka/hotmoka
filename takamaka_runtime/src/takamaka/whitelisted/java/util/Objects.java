package takamaka.whitelisted.java.util;

import takamaka.lang.MustRedefineHashcode;

public interface Objects {
	<T> T requireNonNull(T obj);
	<T> T requireNonNull(T obj, java.lang.String message);
	boolean isNull(java.lang.Object obj);
	boolean nonNull(java.lang.Object obj);
	<T> T requireNonNullElse(T obj, T defaultObj);
	java.lang.String toString(@MustRedefineHashcode java.lang.Object o);
}