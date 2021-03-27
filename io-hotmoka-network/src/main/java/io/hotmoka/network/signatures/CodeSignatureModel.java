package io.hotmoka.network.signatures;

import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.types.StorageType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The model of the signature of a method or constructor.
 */
public abstract class CodeSignatureModel extends SignatureModel {

	/**
	 * The formal arguments of the method or constructor.
	 */
	public List<String> formals;

	/**
	 * Builds the model of the signature of a method or constructor.
	 * 
	 * @param signature the original signature to copy
	 */
	protected CodeSignatureModel(CodeSignature signature) {
		super(signature.definingClass.name);

		this.formals = signature.formals().map(CodeSignatureModel::nameOf).collect(Collectors.toList());
	}

	public CodeSignatureModel() {}


	/**
	 * Yields the storage types of the formal arguments of this method or constructor.
	 * 
	 * @return the storage types
	 */
	protected final StorageType[] getFormalsAsTypes() {
		return this.formals.stream().map(SignatureModel::typeWithName).toArray(StorageType[]::new);
	}
}