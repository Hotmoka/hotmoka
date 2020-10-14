package io.takamaka.code.constants;

/**
 * The names of some classes of the Takamaka library.
 */
public interface Constants {

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.Account}.
	 */
	public final static String ACCOUNT_NAME = "io.takamaka.code.lang.Account";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static String EOA_NAME = "io.takamaka.code.lang.ExternallyOwnedAccount";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RedGreenExternallyOwnedAccount}.
	 */
	public final static String RGEOA_NAME = "io.takamaka.code.lang.RedGreenExternallyOwnedAccount";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.TestExternallyOwnedAccount}.
	 */
	public final static String TEOA_NAME = "io.takamaka.code.lang.TestExternallyOwnedAccount";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount}.
	 */
	public final static String TRGEOA_NAME = "io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static String CONTRACT_NAME = "io.takamaka.code.lang.Contract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RedGreenContract}.
	 */
	public final static String RGCONTRACT_NAME = "io.takamaka.code.lang.RedGreenContract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static String PAYABLE_CONTRACT_NAME = "io.takamaka.code.lang.PayableContract";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static String STORAGE_MAP_NAME = "io.takamaka.code.util.StorageMap";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageList}.
	 */
	public final static String STORAGE_LIST_NAME = "io.takamaka.code.util.StorageList";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageMap.Node}.
	 */
	public final static String STORAGE_MAP_NODE_NAME = "io.takamaka.code.util.StorageMap$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageList.Node}.
	 */
	public final static String STORAGE_LIST_NODE_NAME = "io.takamaka.code.util.StorageList$Node";

	/**
	 * The name of the package prefix of all Takamaka implementation classes.
	 */
	public final static String TAKAMAKA_CODE_PACKAGE = Constants.class.getPackageName().substring(0, Constants.class.getPackageName().indexOf(".constants"));

	/**
	 * The name of the package of the Takamaka language classes.
	 */
	public final static String TAKAMAKA_CODELANG_PACKAGE = TAKAMAKA_CODE_PACKAGE + ".lang";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RedGreenPayableContract}.
	 */
	public final static String RGPAYABLE_CONTRACT_NAME = TAKAMAKA_CODELANG_PACKAGE + ".RedGreenPayableContract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static String STORAGE_NAME = TAKAMAKA_CODELANG_PACKAGE + ".Storage";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.Manifest}.
	 */
	public final static String MANIFEST_NAME = "io.takamaka.code.system.Manifest";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static String PAYABLE_NAME = TAKAMAKA_CODELANG_PACKAGE + ".Payable";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RedPayable}.
	 */
	public final static String RED_PAYABLE_NAME = TAKAMAKA_CODELANG_PACKAGE + ".RedPayable";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Entry}.
	 */
	public final static String ENTRY_NAME = TAKAMAKA_CODELANG_PACKAGE + ".Entry";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static String THROWS_EXCEPTIONS_NAME = TAKAMAKA_CODELANG_PACKAGE + ".ThrowsExceptions";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.SelfCharged}.
	 */
	public final static String SELF_CHARGED_NAME = TAKAMAKA_CODELANG_PACKAGE + ".SelfCharged";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static String EVENT_NAME = TAKAMAKA_CODELANG_PACKAGE + ".Event";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static String VIEW_NAME = TAKAMAKA_CODELANG_PACKAGE + ".View";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static String TAKAMAKA_NAME = TAKAMAKA_CODELANG_PACKAGE + ".Takamaka";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RequirementViolationException}.
	 */
	public final static String REQUIREMENT_VIOLATION_EXCEPTION_NAME = "io.takamaka.code.lang.RequirementViolationException";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.InsufficientFundsError}.
	 */
	public final static String INSUFFICIENT_FUNDS_ERROR_NAME = "io.takamaka.code.lang.InsufficientFundsError";
}