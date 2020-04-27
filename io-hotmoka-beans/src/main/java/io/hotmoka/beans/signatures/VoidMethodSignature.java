package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The signature of a method of a class, that does not return any value.
 */
@Immutable
public final class VoidMethodSignature extends MethodSignature {
	final static byte SELECTOR = 2;

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
		this(new ClassType(definingClass), methodName, formals);
	}

	@Override
	public String toString() {
		return "void " + definingClass + "." + methodName + commaSeparatedFormals();
	};

	@Override
	public boolean equals(Object other) {
		return other instanceof VoidMethodSignature && super.equals(other);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
	}
}