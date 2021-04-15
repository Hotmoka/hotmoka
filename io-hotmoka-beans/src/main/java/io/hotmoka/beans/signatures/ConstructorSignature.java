package io.hotmoka.beans.signatures;

import java.io.IOException;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The signature of a constructor of a class.
 */
@Immutable
public final class ConstructorSignature extends CodeSignature {
	final static byte SELECTOR = 0;
	final static byte SELECTOR_EOA = 3;

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
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof ConstructorSignature && super.equals(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (equals(EOA_CONSTRUCTOR))
			context.writeByte(SELECTOR_EOA);
		else {
			context.writeByte(SELECTOR);
			super.into(context);
		}
	}
}