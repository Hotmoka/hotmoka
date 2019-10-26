package takamaka.blockchain;

import io.takamaka.code.annotations.Immutable;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;

/**
 * The signature of a method of a class, that returns a value.
 */
@Immutable
public final class NonVoidMethodSignature extends MethodSignature {

	private static final long serialVersionUID = 4225754124118472707L;

	/**
	 * The type of the returned type;
	 */
	public final StorageType returnType;

	/**
	 * Builds the signature of a method, that returns a value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 */
	public NonVoidMethodSignature(ClassType definingClass, String methodName, StorageType returnType, StorageType... formals) {
		super(definingClass, methodName, formals);

		this.returnType = returnType;
	}

	/**
	 * Builds the signature of a method, that returns no value.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 */
	public NonVoidMethodSignature(String definingClass, String methodName, StorageType returnType, StorageType... formals) {
		this(ClassType.mk(definingClass), methodName, returnType, formals);
	}

	@Override
	public String toString() {
		return returnType + " " + definingClass + "." + methodName + commaSeparatedFormals();
	};

	@Override
	public boolean equals(Object other) {
		return other instanceof NonVoidMethodSignature && returnType.equals(((NonVoidMethodSignature) other).returnType) && super.equals(other);
	}
}