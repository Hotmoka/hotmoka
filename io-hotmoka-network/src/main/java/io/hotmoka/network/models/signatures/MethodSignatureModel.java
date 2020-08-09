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
	public String methodName;

	/**
	 * The return type of the method, if any.
	 */
	public String returnType;

	/**
	 * Builds the model of the signature of a method.
	 * 
	 * @param method the method original signature to copy
	 */
	public MethodSignatureModel(MethodSignature method) {
		super(method);

		this.methodName = method.methodName;
		if (method instanceof NonVoidMethodSignature)
			returnType = nameOf(((NonVoidMethodSignature) method).returnType);
		else
			returnType = null;
	}

	public MethodSignatureModel() {}

	/**
	 * Yields the method signature corresponding to this model.
	 * 
	 * @return the method signature
	 */
	public MethodSignature toBean() {
		if (returnType == null)
			return new VoidMethodSignature(definingClass, methodName, getFormalsAsTypes());
		else
			return new NonVoidMethodSignature(definingClass, methodName, typeWithName(returnType), getFormalsAsTypes());
	}
}