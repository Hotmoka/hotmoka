package io.hotmoka.network.models.signatures;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.ConstructorSignature;

/**
 * The model of the signature of a constructor of a class.
 */
@Immutable
public final class ConstructorSignatureModel extends CodeSignatureModel {

	/**
	 * Builds the model from the signature of a constructor.
	 * 
	 * @param constructor the signature to copy
	 */
	public ConstructorSignatureModel(ConstructorSignature constructor) {
		super(constructor);
	}

	/**
	 * Yields the constructor signature corresponding to this model.
	 * 
	 * @return the constructor signature
	 */
	public ConstructorSignature toBean() {
		return new ConstructorSignature(definingClass, getFormalsAsTypes());
	}
}