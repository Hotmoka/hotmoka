package io.takamaka.code.whitelisting.internal.database.java.lang;

import io.takamaka.code.whitelisting.MustRedefineHashCode;
import io.takamaka.code.whitelisting.MustRedefineHashCodeOrToString;

public abstract class Object {
	public Object() {}
	public abstract java.lang.Object clone();
	//public abstract java.lang.Class<?> getClass(); // this needs a special treatment in the class verifier
	public abstract boolean equals(java.lang.Object other);
	public abstract @MustRedefineHashCodeOrToString java.lang.String toString();
	public abstract @MustRedefineHashCode int hashCode();
}