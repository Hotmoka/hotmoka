package takamaka.whitelisted.java.lang;

import takamaka.lang.MustRedefineHashcode;

public abstract class Object {
	public Object() {}
	public abstract boolean equals(java.lang.Object other);
	public abstract @MustRedefineHashcode java.lang.String toString();
}