package takamaka.whitelisted.java.lang;

import takamaka.lang.MustRedefineHashCode;
import takamaka.lang.MustRedefineHashCodeOrToString;

public abstract class Object {
	public Object() {}
	public abstract java.lang.Object clone();
	//public abstract java.lang.Class<?> getClass(); // this needs a special treatment in the class verifier
	public abstract boolean equals(java.lang.Object other);
	public abstract @MustRedefineHashCodeOrToString java.lang.String toString();
	public abstract @MustRedefineHashCode int hashCode();
}