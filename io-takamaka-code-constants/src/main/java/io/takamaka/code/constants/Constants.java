package io.takamaka.code.constants;

/**
 * The names of some classes of the Takamaka library.
 */
public interface Constants {

	/**
	 * The name of the class type for {@code io.hotmoka.local.internal.runtime.Runtime}.
	 */
	public final static String RUNTIME_NAME = "io.hotmoka.local.internal.runtime.Runtime";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.Account}.
	 */
	public final static String ACCOUNT_NAME = "io.takamaka.code.lang.Account";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.AccountSHA256DSA}.
	 */
	public final static String ACCOUNT_SHA256DSA_NAME = "io.takamaka.code.lang.AccountSHA256DSA";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.AccountQTESLA1}.
	 */
	public final static String ACCOUNT_QTESLA1_NAME = "io.takamaka.code.lang.AccountQTESLA1";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.AccountQTESLA3}.
	 */
	public final static String ACCOUNT_QTESLA3_NAME = "io.takamaka.code.lang.AccountQTESLA3";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.AccountED25519}.
	 */
	public final static String ACCOUNT_ED25519_NAME = "io.takamaka.code.lang.AccountED25519";

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
	 * The name of the class type for {@link io.takamaka.code.util.StorageMapView}.
	 */
	public final static String STORAGE_MAP_VIEW_NAME = "io.takamaka.code.util.StorageMapView";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static String STORAGE_MAP_NAME = "io.takamaka.code.util.StorageMap";
	
	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageArray}.
	 */
	public final static String STORAGE_ARRAY_NAME = "io.takamaka.code.util.StorageArray";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageListView}.
	 */
	public final static String STORAGE_LIST_VIEW_NAME = "io.takamaka.code.util.StorageListView";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageList}.
	 */
	public final static String STORAGE_LIST_NAME = "io.takamaka.code.util.StorageList";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static String STORAGE_TREE_MAP_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static String STORAGE_LINKED_LIST_NAME = "io.takamaka.code.util.StorageLinkedList";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	public final static String STORAGE_LINKED_LIST_NODE_NAME = "io.takamaka.code.util.StorageLinkedList$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.ConsensusUpdate}.
	 */
	public final static String CONSENSUS_UPDATE_NAME = "io.takamaka.code.system.ConsensusUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.GasPriceUpdate}.
	 */
	public final static String GAS_PRICE_UPDATE_NAME = "io.takamaka.code.system.GasPriceUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.VerificationVersionUpdate}.
	 */
	public final static String VERIFICATION_VERSION_UPDATE_NAME = "io.takamaka.code.system.VerificationVersionUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.ValidatorsUpdate}.
	 */
	public final static String VALIDATORS_UPDATE_NAME = "io.takamaka.code.system.ValidatorsUpdate";

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
	 * The name of the class type for {@link io.takamaka.code.system.Validator}.
	 */
	public final static String VALIDATOR_NAME = "io.takamaka.code.system.Validator";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.Validators}.
	 */
	public final static String VALIDATORS_NAME = "io.takamaka.code.system.Validators";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.Versions}.
	 */
	public final static String VERSIONS_NAME = "io.takamaka.code.system.Versions";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.GasStation}.
	 */
	public final static String GAS_STATION_NAME = "io.takamaka.code.system.GasStation";

	/**
	 * The name of the class type for {@link io.takamaka.code.system.tendermint.TendermintValidators}.
	 */
	public final static String TENDERMINT_VALIDATORS_NAME = "io.takamaka.code.system.tendermint.TendermintValidators";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static String PAYABLE_NAME = TAKAMAKA_CODELANG_PACKAGE + ".Payable";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RedPayable}.
	 */
	public final static String RED_PAYABLE_NAME = TAKAMAKA_CODELANG_PACKAGE + ".RedPayable";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	public final static String FROM_CONTRACT_NAME = TAKAMAKA_CODELANG_PACKAGE + ".FromContract";

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
	 * The name of the class type for {@link io.takamaka.code.lang.Exported}.
	 */
	public final static String EXPORTED_NAME = TAKAMAKA_CODELANG_PACKAGE + ".Exported";

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

	/**
	 * The default value of verification version
	 */
	public final static int DEFAULT_VERIFICATION_VERSION = 0;

}