package io.takamaka.code.whitelisting.internal.database.java.lang;

public abstract class Enum {
	public Enum(java.lang.String name, int ordinal) {}
	public abstract java.lang.Enum<?> valueOf(java.lang.Class<?> clazz, java.lang.String name);
	public abstract int ordinal();
	public abstract boolean equals(java.lang.Object other);
	public abstract int hashCode();
	public abstract java.lang.String toString();
}