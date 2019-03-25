package takamaka.translator;

/**
 * This class is used only in the instrumentation of an {@code @@Entry} method or constructor,
 * as an extra type added at the end of its signature: m(formals) becomes
 * m(formals, Contract, Dummy). The goal is to avoid signature clashes
 * because of the instrumentation: since {@code Dummy} is not white-listed, it cannot
 * be used by the programmer and the instrumentation cannot lead to signature clashes.
 * The value passed at run time for this parameter will always be {@code null}.
 */
public final class Dummy {
	private Dummy() {}
}