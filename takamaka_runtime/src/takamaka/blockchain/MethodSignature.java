package takamaka.blockchain;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.lang.Immutable;

/**
 * The signature of a method of a class.
 */
@Immutable
public final class MethodSignature extends CodeSignature {

	/**
	 * The name of the method.
	 */
	public final String methodName;

	/**
	 * Builds the signature of a method.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	public MethodSignature(ClassType definingClass, String methodName, StorageType... formals) {
		super(definingClass, formals);

		this.methodName = methodName;
	}

	/**
	 * Builds the signature of a method.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	public MethodSignature(String definingClass, String methodName, StorageType... formals) {
		this(new ClassType(definingClass), methodName, formals);
	}

	@Override
	public String toString() {
		return definingClass + "." + methodName + commaSeparatedFormals();
	};

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodSignature && methodName.equals(((MethodSignature) other).methodName) && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ methodName.hashCode();
	}
}