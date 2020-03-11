package io.takamaka.code.whitelisting.internal.database.java.lang;

public abstract class Enum {
	public Enum(java.lang.String name, int ordinal) {}
	public abstract java.lang.Enum<?> valueOf(java.lang.Class<?> clazz, java.lang.String name);
	public abstract int ordinal();
}