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
	public abstract boolean endsWith(java.lang.String suffix);
	public abstract boolean startsWith(java.lang.String prefix);
	public abstract boolean contains(java.lang.CharSequence what);
	public abstract java.lang.String toLowerCase();
	public abstract java.lang.String toUpperCase();
	public abstract java.lang.String[] split(java.lang.String s);
	public abstract int indexOf(int c);
	public abstract java.lang.String substring(int begin, int end);
	public abstract java.lang.String substring(int begin);
}