package io.takamaka.code.whitelisting.internal.database.java.lang;

import io.takamaka.code.whitelisting.MustRedefineHashCodeOrToString;

public abstract class String {
	public String(java.lang.String original) {};
	public abstract int length();
	public abstract java.lang.String valueOf(int i);
	public abstract java.lang.String valueOf(@MustRedefineHashCodeOrToString java.lang.Object obj);
	public abstract java.lang.String concat(java.lang.String other);
}