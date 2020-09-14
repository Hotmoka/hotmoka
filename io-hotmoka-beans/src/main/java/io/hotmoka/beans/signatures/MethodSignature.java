package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The signature of a method of a class.
 */
@Immutable
public abstract class MethodSignature extends CodeSignature {

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
	protected MethodSignature(ClassType definingClass, String methodName, StorageType... formals) {
		super(definingClass, formals);

		if (methodName == null)
			throw new IllegalArgumentException("methodName cannot be null");

		this.methodName = methodName;
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

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(methodName));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		super.into(context);
		context.oos.writeUTF(methodName);
	}
}