package io.hotmoka.network.signatures;

import io.hotmoka.beans.signatures.ConstructorSignature;

/**
 * The model of the signature of a constructor of a class.
 */
public final class ConstructorSignatureModel extends CodeSignatureModel {

	/**
	 * Builds the model from the signature of a constructor.
	 * 
	 * @param constructor the signature to copy
	 */
	public ConstructorSignatureModel(ConstructorSignature constructor) {
		super(constructor);
	}

	public ConstructorSignatureModel() {}

	/**
	 * Yields the constructor signature corresponding to this model.
	 * 
	 * @return the constructor signature
	 */
	public ConstructorSignature toBean() {
		return new ConstructorSignature(definingClass, getFormalsAsTypes());
	}
}