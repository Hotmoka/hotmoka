/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.constants;

/**
 * The names of some classes of the Takamaka library.
 */
public interface Constants {

	/**
	 * The version of the Hotmoka code.
	 */
	String VERSION = "1.0.9";

	/**
	 * The name of the class {@code io.hotmoka.local.internal.runtime.Runtime}.
	 */
	String RUNTIME_NAME = "io.hotmoka.local.internal.runtime.Runtime";

	/**
	 * The name of the class {@code io.hotmoka.verification.Dummy}.
	 */
	String DUMMY_NAME = "io.hotmoka.verification.Dummy";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.Account}.
	 */
	String ACCOUNT_NAME = "io.takamaka.code.lang.Account";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.Accounts}.
	 */
	String ACCOUNTS_NAME = "io.takamaka.code.lang.Accounts";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.ExternallyOwnedAccounts}.
	 */
	String EXTERNALLY_OWNED_ACCOUNTS_NAME = "io.takamaka.code.lang.ExternallyOwnedAccounts";

	/**
	 * The name of the interface type for {@link io.takamaka.code.tokens.IERC20}.
	 */
	String IERC20_NAME = "io.takamaka.code.tokens.IERC20";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.AccountSHA256DSA}.
	 */
	String ACCOUNT_SHA256DSA_NAME = "io.takamaka.code.lang.AccountSHA256DSA";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.AccountQTESLA1}.
	 */
	String ACCOUNT_QTESLA1_NAME = "io.takamaka.code.lang.AccountQTESLA1";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.AccountQTESLA3}.
	 */
	String ACCOUNT_QTESLA3_NAME = "io.takamaka.code.lang.AccountQTESLA3";

	/**
	 * The name of the interface type for {@link io.takamaka.code.lang.AccountED25519}.
	 */
	String ACCOUNT_ED25519_NAME = "io.takamaka.code.lang.AccountED25519";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	String EOA_NAME = "io.takamaka.code.lang.ExternallyOwnedAccount";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountED25519}.
	 */
	String EOA_ED25519_NAME = "io.takamaka.code.lang.ExternallyOwnedAccountED25519";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountED25519}.
	 */
	String EOA_SHA256DSA_NAME = "io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1}.
	 */
	String EOA_QTESLA1_NAME = "io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3}.
	 */
	String EOA_QTESLA3_NAME = "io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Gamete}.
	 */
	String GAMETE_NAME = "io.takamaka.code.lang.Gamete";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.AbstractValidators}.
	 */
	String ABSTRACT_VALIDATORS_NAME = "io.takamaka.code.governance.AbstractValidators";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Contract}.
	 */
	String CONTRACT_NAME = "io.takamaka.code.lang.Contract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	String PAYABLE_CONTRACT_NAME = "io.takamaka.code.lang.PayableContract";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageMapView}.
	 */
	String STORAGE_MAP_VIEW_NAME = "io.takamaka.code.util.StorageMapView";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeMap}.
	 */
	String STORAGE_TREE_MAP_NAME = "io.takamaka.code.util.StorageTreeMap";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeArray}.
	 */
	String STORAGE_TREE_ARRAY_NAME = "io.takamaka.code.util.StorageTreeArray";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeArray.Node}.
	 */
	String STORAGE_TREE_ARRAY_NODE_NAME = "io.takamaka.code.util.StorageTreeArray$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeIntMap}.
	 */
	String STORAGE_TREE_INTMAP_NAME = "io.takamaka.code.util.StorageTreeIntMap";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeSet}.
	 */
	String STORAGE_TREE_SET_NAME = "io.takamaka.code.util.StorageTreeSet";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeMap.BlackNode}.
	 */
	String STORAGE_TREE_MAP_BLACK_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$BlackNode";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeMap.RedNode}.
	 */
	String STORAGE_TREE_MAP_RED_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$RedNode";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageSetView}.
	 */
	String STORAGE_SET_VIEW_NAME = "io.takamaka.code.util.StorageSetView";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	String STORAGE_MAP_NAME = "io.takamaka.code.util.StorageMap";
	
	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageArray}.
	 */
	String STORAGE_ARRAY_NAME = "io.takamaka.code.util.StorageArray";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageListView}.
	 */
	String STORAGE_LIST_VIEW_NAME = "io.takamaka.code.util.StorageListView";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageList}.
	 */
	String STORAGE_LIST_NAME = "io.takamaka.code.util.StorageList";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	String STORAGE_TREE_MAP_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	String STORAGE_TREE_INTMAP_NODE_NAME = "io.takamaka.code.util.StorageTreeIntMap$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	String STORAGE_LINKED_LIST_NAME = "io.takamaka.code.util.StorageLinkedList";

	/**
	 * The name of the class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	String STORAGE_LINKED_LIST_NODE_NAME = "io.takamaka.code.util.StorageLinkedList$Node";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.ConsensusUpdate}.
	 */
	String CONSENSUS_UPDATE_NAME = "io.takamaka.code.governance.ConsensusUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.GasPriceUpdate}.
	 */
	String GAS_PRICE_UPDATE_NAME = "io.takamaka.code.governance.GasPriceUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.InflationUpdate}.
	 */
	String INFLATION_UPDATE_NAME = "io.takamaka.code.governance.InflationUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.VerificationVersionUpdate}.
	 */
	String VERIFICATION_VERSION_UPDATE_NAME = "io.takamaka.code.governance.VerificationVersionUpdate";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.ValidatorsUpdate}.
	 */
	String VALIDATORS_UPDATE_NAME = "io.takamaka.code.governance.ValidatorsUpdate";

	/**
	 * The name of the package prefix of all Takamaka implementation classes.
	 */
	String IO_TAKAMAKA_CODE_PACKAGE_NAME = "io.takamaka.code.";

	/**
	 * The name of the package of the Takamaka language classes.
	 */
	String IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + "lang.";
	
	/**
	 * The name of the package of the Takamaka utility classes.
	 */
	String IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + "util.";

	/**
	 * The name of the package of the Takamaka tokens classes.
	 */
	String IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + "tokens.";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Storage}.
	 */
	String STORAGE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Storage";

	/**
	 * The name of the class type for {@link io.takamaka.code.math.UnsignedBigInteger}.
	 */
	String UNSIGNED_BIG_INTEGER_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + "math.UnsignedBigInteger";

	/**
	 * The name of the class type for {@link io.takamaka.code.tokens.ERC20}.
	 */
	String ERC20_NAME = IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME + "ERC20";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.Manifest}.
	 */
	String MANIFEST_NAME = "io.takamaka.code.governance.Manifest";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.Validator}.
	 */
	String VALIDATOR_NAME = "io.takamaka.code.governance.Validator";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.Validators}.
	 */
	String VALIDATORS_NAME = "io.takamaka.code.governance.Validators";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.Versions}.
	 */
	String VERSIONS_NAME = "io.takamaka.code.governance.Versions";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.AccountsLedger}.
	 */
	String ACCOUNTS_LEDGER_NAME = "io.takamaka.code.governance.AccountsLedger";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.GasStation}.
	 */
	String GAS_STATION_NAME = "io.takamaka.code.governance.GasStation";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.GenericGasStation}.
	 */
	String GENERIC_GAS_STATION_NAME = "io.takamaka.code.governance.GenericGasStation";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.tendermint.TendermintValidators}.
	 */
	String TENDERMINT_VALIDATORS_NAME = "io.takamaka.code.governance.tendermint.TendermintValidators";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.tendermint.TendermintED25519Validator}.
	 */
	String TENDERMINT_ED25519_VALIDATOR_NAME = "io.takamaka.code.governance.tendermint.TendermintED25519Validator";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Payable}.
	 */
	String PAYABLE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Payable";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RedPayable}.
	 */
	String RED_PAYABLE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "RedPayable";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	String FROM_CONTRACT_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "FromContract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	String THROWS_EXCEPTIONS_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "ThrowsExceptions";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.SelfCharged}.
	 */
	String SELF_CHARGED_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "SelfCharged";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Event}.
	 */
	String EVENT_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Event";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.View}.
	 */
	String VIEW_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "View";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Exported}.
	 */
	String EXPORTED_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Exported";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	String TAKAMAKA_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Takamaka";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.RequirementViolationException}.
	 */
	String REQUIREMENT_VIOLATION_EXCEPTION_NAME = "io.takamaka.code.lang.RequirementViolationException";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.InsufficientFundsError}.
	 */
	String INSUFFICIENT_FUNDS_ERROR_NAME = "io.takamaka.code.lang.InsufficientFundsError";

	/**
	 * The name of the class type for {@link io.takamaka.code.governance.GenericValidators}.
	 */
	String GENERIC_VALIDATORS_NAME = "io.takamaka.code.governance.GenericValidators";
	
	/**
	 * The name of the class type for {@link io.takamaka.code.dao.Poll}.
	 */
	String POLL_NAME = "io.takamaka.code.dao.Poll";
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity}.
	 */
	String SHARED_ENTITY_NAME = "io.takamaka.code.dao.SharedEntity";

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity.Offer}.
	 */
	String SHARED_ENTITY_OFFER_NAME = "io.takamaka.code.dao.SharedEntity$Offer";

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntityView}.
	 */
	String SHARED_ENTITY_VIEW_NAME = "io.takamaka.code.dao.SharedEntityView";
}