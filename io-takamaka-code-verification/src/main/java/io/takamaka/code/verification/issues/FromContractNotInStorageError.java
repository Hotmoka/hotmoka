package io.takamaka.code.verification.issues;

public class FromContractNotInStorageError extends Error {

	public FromContractNotInStorageError(String where, String methodName) {
		super(where, methodName, -1, "@FromContract can only be applied to constructors or instance methods of a storage class or of an interface");
	}
}