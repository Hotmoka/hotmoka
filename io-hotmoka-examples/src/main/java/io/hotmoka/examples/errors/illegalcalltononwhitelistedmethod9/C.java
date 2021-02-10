package io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod9;

public class C {

	@Override
	public int hashCode() {
		// this is illegal, although the receiver redefines hashCode():
		// the reason is that the @MustRedefineHashCode annotation on a method
		// does not allow that method to be called through invokespecial, with
		// a target method whose static resolution is Object.hashCode();
		// in other terms, the check for the redefinition of hashCode() is done
		// statically, at verification time, from the declared type of the receiver
		return super.hashCode();
	}
}