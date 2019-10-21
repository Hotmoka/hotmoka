package takamaka.instrumentation;

/**
 * This class is used only in the instrumentation of an entry method or constructor,
 * as an extra type added at the end of its signature: {@code m(formals)} becomes
 * {@code m(formals, Contract, Dummy)}, where the {@link takamaka.lang.Contract}
 * is the caller of the entry. The goal is to avoid signature clashes
 * because of the instrumentation: since {@link takamaka.instrumentation.Dummy} is not white-listed, it cannot
 * be used by the programmer and the instrumentation cannot lead to signature clashes.
 * The value passed at run time for this parameter will always be {@code null}.
 */
public final class Dummy {
	private Dummy() {}
}