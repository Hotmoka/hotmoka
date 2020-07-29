package io.hotmoka.network.models.signatures;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.types.StorageType;

/**
 * The model of the signature of a method or constructor.
 */
@Immutable
public abstract class CodeSignatureModel extends SignatureModel {

	/**
	 * The formal arguments of the method or constructor.
	 */
	private final List<String> formals;

	/**
	 * Builds the model of the signature of a method or constructor.
	 * 
	 * @param signature the original signature to copy
	 */
	protected CodeSignatureModel(CodeSignature signature) {
		super(signature.definingClass.name);

		this.formals = signature.formals().map(CodeSignatureModel::nameOf).collect(Collectors.toList());
	}

	public final Stream<String> getFormals() {
		return formals.stream();
	}

	/**
	 * Yields the storage types of the formal arguments of this method or constructor.
	 * 
	 * @return the storage types
	 */
	protected final StorageType[] getFormalsAsTypes() {
		return getFormals().map(SignatureModel::typeWithName).toArray(StorageType[]::new);
	}
}