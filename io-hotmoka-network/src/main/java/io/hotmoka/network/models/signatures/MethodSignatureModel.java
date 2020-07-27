package io.hotmoka.network.models.signatures;

import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;

/**
 * The model of the signature of a method of a class.
 */
public class MethodSignatureModel extends CodeSignatureModel {

	/**
	 * The name of the method.
	 */
	private String methodName;

	/**
	 * The return type of the method, if any.
	 */
	private String returnType;

	/**
	 * For Spring.
	 */
	public MethodSignatureModel() {}

	/**
	 * Builds the model of the signature of a method.
	 * 
	 * @param parent the original signature to copy
	 */
	public MethodSignatureModel(MethodSignature method) {
		super(method);

		this.methodName = method.methodName;
		if (method instanceof NonVoidMethodSignature)
			returnType = nameOf(((NonVoidMethodSignature) method).returnType);
	}

	/**
	 * Yields the method signature corresponding to this model.
	 * 
	 * @return the method signature
	 */
	public MethodSignature toBean() {
		if (returnType == null)
			return new VoidMethodSignature(getDefiningClass(), methodName, getFormalsAsTypes());
		else
			return new NonVoidMethodSignature(getDefiningClass(), methodName, typeWithName(returnType), getFormalsAsTypes());
	}

	/**
	 * For Spring.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * For Spring.
	 */
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
}