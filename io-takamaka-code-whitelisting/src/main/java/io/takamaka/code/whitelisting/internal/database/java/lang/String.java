package io.takamaka.code.whitelisting.internal.database.java.lang;

import io.takamaka.code.whitelisting.HasDeterministicTerminatingToString;

public abstract class String {
	public String(java.lang.String original) {};
	public abstract int length();
	public abstract boolean isEmpty();
	public abstract boolean equals(java.lang.Object other);
	public abstract int hashCode();
	public abstract java.lang.String toString();
	public abstract java.lang.String valueOf(int i);
	public abstract java.lang.String valueOf(@HasDeterministicTerminatingToString java.lang.Object obj);
	public abstract java.lang.String concat(java.lang.String other);
}