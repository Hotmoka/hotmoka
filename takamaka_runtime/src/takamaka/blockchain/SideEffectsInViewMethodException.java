package takamaka.blockchain;

/**
 * An exception thrown when a transaction for the execution of a
 * {@link takamaka.lang.View} method has side-effects different
 * from the modification of the balance of the caller.
 */
@SuppressWarnings("serial")
public class SideEffectsInViewMethodException extends Exception {

	public SideEffectsInViewMethodException(MethodSignature method) {
		super("Method " + method + " induced side-effects");
	}
}