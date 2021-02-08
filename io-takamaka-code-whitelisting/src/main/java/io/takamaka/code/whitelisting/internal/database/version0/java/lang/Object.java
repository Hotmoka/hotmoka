package io.takamaka.code.whitelisting.internal.database.version0.java.lang;

import io.takamaka.code.whitelisting.HasDeterministicTerminatingEquals;
import io.takamaka.code.whitelisting.HasDeterministicTerminatingHashCode;
import io.takamaka.code.whitelisting.HasDeterministicTerminatingToString;

public abstract class Object {
	public Object() {}
	public abstract java.lang.Object clone();
	//public abstract java.lang.Class<?> getClass(); // this needs a special treatment in the class verifier
	public abstract @HasDeterministicTerminatingEquals boolean equals(java.lang.Object other);
	public abstract @HasDeterministicTerminatingToString java.lang.String toString();
	public abstract @HasDeterministicTerminatingHashCode int hashCode();
}