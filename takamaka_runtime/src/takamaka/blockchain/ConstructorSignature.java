package takamaka.blockchain;

import io.takamaka.code.annotations.Immutable;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;

/**
 * The signature of a constructor of a class.
 */
@Immutable
public final class ConstructorSignature extends CodeSignature {

	private static final long serialVersionUID = -3069430655042860986L;

	/**
	 * Builds the signature of a constructor.
	 * 
	 * @param definingClass the class of the constructor
	 * @param formals the formal arguments of the constructor
	 */
	public ConstructorSignature(ClassType definingClass, StorageType... formals) {
		super(definingClass, formals);
	}

	/**
	 * Builds the signature of a constructor.
	 * 
	 * @param definingClass the name of the class of the constructor
	 * @param formals the formal arguments of the constructor
	 */
	public ConstructorSignature(String definingClass, StorageType... formals) {
		super(definingClass, formals);
	}

	@Override
	public String toString() {
		return definingClass + commaSeparatedFormals();
	};

	@Override
	public boolean equals(Object other) {
		return other instanceof ConstructorSignature && super.equals(other);
	}
}