package io.takamaka.code.verification;

/**
 * A collector of constants useful during code verification.
 */
public interface Constants {

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static String EOA_NAME = "io.takamaka.code.lang.ExternallyOwnedAccount";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.TestExternallyOwnedAccount}.
	 */
	public final static String TEOA_NAME = "io.takamaka.code.lang.TestExternallyOwnedAccount";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static String CONTRACT_NAME = "io.takamaka.code.lang.Contract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static String PAYABLE_CONTRACT_NAME = "io.takamaka.code.lang.PayableContract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static String STORAGE_NAME = "io.takamaka.code.lang.Storage";

	/**
	 * The name of the class type for {@link io.takamaka.code.blockchain.values.StorageReference}.
	 */
	public final static String STORAGE_REFERENCE_NAME = "io.takamaka.code.blockchain.values.StorageReference";

	/**
	 * The name of the class type for {@link io.takamaka.code.blockchain.values.StorageValue}.
	 */
	public final static String STORAGE_VALUE_NAME = "io.takamaka.code.blockchain.values.StorageValue";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static String PAYABLE_NAME = "io.takamaka.code.lang.Payable";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Entry}.
	 */
	public final static String ENTRY_NAME = "io.takamaka.code.lang.Entry";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ThrowsException}.
	 */
	public final static String THROWS_EXCEPTIONS_NAME = "io.takamaka.code.lang.ThrowsExceptions";

	/**
	 * This character is forbidden in the name of fields and methods of Takamaka code,
	 * since it will be used for instrumentation. Java compilers do not allow one to
	 * use this character in the name of fields or methods, but it is still possible if
	 * Java bytecode is produced in other ways. Hence it is necessary to check that it is not used.
	 */
	public final static char FORBIDDEN_PREFIX = '§';
}