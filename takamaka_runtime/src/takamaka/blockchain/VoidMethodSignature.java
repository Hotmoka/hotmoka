package takamaka.blockchain;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;

/**
 * The signature of a method of a class, that does not return any value.
 */
@Immutable
public final class VoidMethodSignature extends MethodSignature {

	private static final long serialVersionUID = 7990082363207586877L;

	/**
	 * Builds the signature of a method, that returns no value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	public VoidMethodSignature(ClassType definingClass, String methodName, StorageType... formals) {
		super(definingClass, methodName, formals);
	}

	/**
	 * Builds the signature of a method, that returns no value.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	public VoidMethodSignature(String definingClass, String methodName, StorageType... formals) {
		this(ClassType.mk(definingClass), methodName, formals);
	}

	@Override
	public String toString() {
		return "void " + definingClass + "." + methodName + commaSeparatedFormals();
	};

	@Override
	public boolean equals(Object other) {
		return other instanceof VoidMethodSignature && super.equals(other);
	}
}