package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The signature of a constructor of a class.
 */
@Immutable
public final class ConstructorSignature extends CodeSignature {

	private static final long serialVersionUID = -3069430655042860986L;
	final static byte SELECTOR = 0;

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

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
	}
}