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
	 * The name of the interface type for {@link io.takamaka.code.lang.Accounts}.
	 */
	public final static String ACCOUNTS_NAME = "io.takamaka.code.lang.Accounts";

	/**
	 * The name of the interface type for {@link io.takamaka.code.tokens.IERC20}.
	 */
	public final static String IERC20_NAME = "io.takamaka.code.tokens.IERC20";

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
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static String STORAGE_TREE_MAP_NAME = "io.takamaka.code.util.StorageTreeMap";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeArray}.
	 */
	public final static String STORAGE_TREE_ARRAY_NAME = "io.takamaka.code.util.StorageTreeArray";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeArray.Node}.
	 */
	public final static String STORAGE_TREE_ARRAY_NODE_NAME = "io.takamaka.code.util.StorageTreeArray$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageIntTreeMap}.
	 */
	public final static String STORAGE_TREE_INTMAP_NAME = "io.takamaka.code.util.StorageTreeIntMap";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeMap.BlackNode}.
	 */
	public final static String STORAGE_TREE_MAP_BLACK_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$BlackNode";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeMap.RedNode}.
	 */
	public final static String STORAGE_TREE_MAP_RED_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$RedNode";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageSetView}.
	 */
	public final static String STORAGE_SET_VIEW_NAME = "io.takamaka.code.util.StorageSetView";

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
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static String STORAGE_TREE_INTMAP_NODE_NAME = "io.takamaka.code.util.StorageTreeIntMap$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static String STORAGE_LINKED_LIST_NAME = "io.takamaka.code.util.StorageLinkedList";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	public final static String STORAGE_LINKED_LIST_NODE_NAME = "io.takamaka.code.util.StorageLinkedList$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.ConsensusUpdate}.
	 */
	public final static String CONSENSUS_UPDATE_NAME = "io.takamaka.code.governance.ConsensusUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.GasPriceUpdate}.
	 */
	public final static String GAS_PRICE_UPDATE_NAME = "io.takamaka.code.governance.GasPriceUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.VerificationVersionUpdate}.
	 */
	public final static String VERIFICATION_VERSION_UPDATE_NAME = "io.takamaka.code.governance.VerificationVersionUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.ValidatorsUpdate}.
	 */
	public final static String VALIDATORS_UPDATE_NAME = "io.takamaka.code.governance.ValidatorsUpdate";

	/**
	 * The name of the package prefix of all Takamaka implementation classes.
	 */
	public final static String IO_TAKAMAKA_CODE_PACKAGE_NAME = Constants.class.getPackageName().substring(0, Constants.class.getPackageName().indexOf(".constants"));

	/**
	 * The name of the package of the Takamaka language classes.
	 */
	public final static String IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + ".lang";
	
	/**
	 * The name of the package of the Takamaka utility classes.
	 */
	public final static String IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + ".util";

	/**
	 * The name of the package of the Takamaka tokens classes.
	 */
	public final static String IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + ".tokens";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RedGreenPayableContract}.
	 */
	public final static String RGPAYABLE_CONTRACT_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".RedGreenPayableContract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static String STORAGE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".Storage";

	/**
	 * The name of the class type for {@link io.takamaka.code.math.UnsignedBigInteger}.
	 */
	public final static String UNSIGNED_BIG_INTEGER_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + ".math.UnsignedBigInteger";

	/**
	 * The name of the class type for {@link io.takamaka.code.tokens.ERC20}.
	 */
	public final static String ERC20_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + ".tokens.ERC20";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.Manifest}.
	 */
	public final static String MANIFEST_NAME = "io.takamaka.code.governance.Manifest";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.Validator}.
	 */
	public final static String VALIDATOR_NAME = "io.takamaka.code.governance.Validator";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.Validators}.
	 */
	public final static String VALIDATORS_NAME = "io.takamaka.code.governance.Validators";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.Versions}.
	 */
	public final static String VERSIONS_NAME = "io.takamaka.code.governance.Versions";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.GasStation}.
	 */
	public final static String GAS_STATION_NAME = "io.takamaka.code.governance.GasStation";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static String GENERIC_GAS_STATION_NAME = "io.takamaka.code.governance.GenericGasStation";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.tendermint.TendermintValidators}.
	 */
	public final static String TENDERMINT_VALIDATORS_NAME = "io.takamaka.code.governance.tendermint.TendermintValidators";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static String PAYABLE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".Payable";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RedPayable}.
	 */
	public final static String RED_PAYABLE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".RedPayable";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	public final static String FROM_CONTRACT_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".FromContract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static String THROWS_EXCEPTIONS_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".ThrowsExceptions";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.SelfCharged}.
	 */
	public final static String SELF_CHARGED_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".SelfCharged";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static String EVENT_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".Event";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static String VIEW_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".View";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Exported}.
	 */
	public final static String EXPORTED_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".Exported";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static String TAKAMAKA_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + ".Takamaka";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RequirementViolationException}.
	 */
	public final static String REQUIREMENT_VIOLATION_EXCEPTION_NAME = "io.takamaka.code.lang.RequirementViolationException";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.InsufficientFundsError}.
	 */
	public final static String INSUFFICIENT_FUNDS_ERROR_NAME = "io.takamaka.code.lang.InsufficientFundsError";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.GenericValidators}.
	 */
	public final static String GENERIC_VALIDATORS_NAME = "io.takamaka.code.governance.GenericValidators";
	
	/**
	 * The name of the class type for {@link io.takamaka.code.dao.Poll}.
	 */
	public final static String POLL_NAME = "io.takamaka.code.dao.Poll";
	
	/**
	 * The default value of verification version
	 */
	public final static int DEFAULT_VERIFICATION_VERSION = 0;

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity}.
	 */
	public static final String SHARED_ENTITY_NAME = "io.takamaka.code.dao.SharedEntity";

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntityView}.
	 */
	public static final String SHARED_ENTITY_VIEW_NAME = "io.takamaka.code.dao.SharedEntityView";
}