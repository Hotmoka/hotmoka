package io.takamaka.code.verification;

/**
 * This class is used only in the instrumentation of an entry method or constructor,
 * as an extra type added at the end of its signature: {@code m(formals)} becomes
 * {@code m(formals, Contract, Dummy)}, where the {@link io.takamaka.code.lang.Contract}
 * is the caller of the entry. The goal is to avoid signature clashes
 * because of the instrumentation: since this class is not white-listed, it cannot
 * be used by the programmer and the instrumentation cannot lead to signature clashes.
 * Moreover, the value passed for this extra parameter can be used to signal something to the callee.
 */
public final class Dummy {
	
	/**
	 * This value is passed to a from contract method to signal that it has
	 * been called on this in the caller.
	 */
	public final static Dummy METHOD_ON_THIS = new Dummy();

	private Dummy() {}
}