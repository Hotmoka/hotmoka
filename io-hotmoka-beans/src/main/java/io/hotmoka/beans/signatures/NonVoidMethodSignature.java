package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The signature of a method of a class, that returns a value.
 */
@Immutable
public final class NonVoidMethodSignature extends MethodSignature {
	final static byte SELECTOR = 1;

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

		if (returnType == null)
			throw new IllegalArgumentException("returnType cannot be null");

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
		this(new ClassType(definingClass), methodName, returnType, formals);
	}

	@Override
	public String toString() {
		return returnType + " " + definingClass + "." + methodName + commaSeparatedFormals();
	};

	@Override
	public boolean equals(Object other) {
		return other instanceof NonVoidMethodSignature && returnType.equals(((NonVoidMethodSignature) other).returnType) && super.equals(other);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(returnType.size(gasCostModel));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		super.into(context);
		returnType.into(context);
	}
}