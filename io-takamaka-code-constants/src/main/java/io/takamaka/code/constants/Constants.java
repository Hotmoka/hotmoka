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

package io.takamaka.code.constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Constants about Takamaka and Hotmoka.
 */
public final class Constants {

	private Constants() {}

	static {
		// we access the Maven properties from the pom.xml file of the parent project
		try (InputStream is = Constants.class.getClassLoader().getResourceAsStream("maven.properties")) {
			var mavenProperties = new Properties();
			mavenProperties.load(is);
			TAKAMAKA_VERSION = mavenProperties.getProperty("io.takamaka.code.version");
		}
		catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * The version of the Takamaka code.
	 */
	public final static String TAKAMAKA_VERSION;

	/**
	 * The name of the interface type for {@code io.takamaka.code.lang.Account}.
	 */
	public final static String ACCOUNT_NAME = "io.takamaka.code.lang.Account";

	/**
	 * The name of the interface type for {@code io.takamaka.code.lang.Accounts}.
	 */
	public final static String ACCOUNTS_NAME = "io.takamaka.code.lang.Accounts";

	/**
	 * The name of the interface type for {@code io.takamaka.code.lang.ExternallyOwnedAccounts}.
	 */
	public final static String EXTERNALLY_OWNED_ACCOUNTS_NAME = "io.takamaka.code.lang.ExternallyOwnedAccounts";

	/**
	 * The name of the interface type for {@code io.takamaka.code.tokens.IERC20}.
	 */
	public final static String IERC20_NAME = "io.takamaka.code.tokens.IERC20";

	/**
	 * The name of the interface type for {@code io.takamaka.code.lang.AccountSHA256DSA}.
	 */
	public final static String ACCOUNT_SHA256DSA_NAME = "io.takamaka.code.lang.AccountSHA256DSA";

	/**
	 * The name of the interface type for {@code io.takamaka.code.lang.AccountQTESLA1}.
	 */
	public final static String ACCOUNT_QTESLA1_NAME = "io.takamaka.code.lang.AccountQTESLA1";

	/**
	 * The name of the interface type for {@code io.takamaka.code.lang.AccountQTESLA3}.
	 */
	public final static String ACCOUNT_QTESLA3_NAME = "io.takamaka.code.lang.AccountQTESLA3";

	/**
	 * The name of the interface type for {@code io.takamaka.code.lang.AccountED25519}.
	 */
	public final static String ACCOUNT_ED25519_NAME = "io.takamaka.code.lang.AccountED25519";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static String EOA_NAME = "io.takamaka.code.lang.ExternallyOwnedAccount";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.ExternallyOwnedAccountED25519}.
	 */
	public final static String EOA_ED25519_NAME = "io.takamaka.code.lang.ExternallyOwnedAccountED25519";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.ExternallyOwnedAccountED25519}.
	 */
	public final static String EOA_SHA256DSA_NAME = "io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1}.
	 */
	public final static String EOA_QTESLA1_NAME = "io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3}.
	 */
	public final static String EOA_QTESLA3_NAME = "io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.Gamete}.
	 */
	public final static String GAMETE_NAME = "io.takamaka.code.lang.Gamete";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.AbstractValidators}.
	 */
	public final static String ABSTRACT_VALIDATORS_NAME = "io.takamaka.code.governance.AbstractValidators";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.Contract}.
	 */
	public final static String CONTRACT_NAME = "io.takamaka.code.lang.Contract";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.PayableContract}.
	 */
	public final static String PAYABLE_CONTRACT_NAME = "io.takamaka.code.lang.PayableContract";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageMapView}.
	 */
	public final static String STORAGE_MAP_VIEW_NAME = "io.takamaka.code.util.StorageMapView";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static String STORAGE_TREE_MAP_NAME = "io.takamaka.code.util.StorageTreeMap";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeArray}.
	 */
	public final static String STORAGE_TREE_ARRAY_NAME = "io.takamaka.code.util.StorageTreeArray";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeArray.Node}.
	 */
	public final static String STORAGE_TREE_ARRAY_NODE_NAME = "io.takamaka.code.util.StorageTreeArray$Node";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeIntMap}.
	 */
	public final static String STORAGE_TREE_INTMAP_NAME = "io.takamaka.code.util.StorageTreeIntMap";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeSet}.
	 */
	public final static String STORAGE_TREE_SET_NAME = "io.takamaka.code.util.StorageTreeSet";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeMap.BlackNode}.
	 */
	public final static String STORAGE_TREE_MAP_BLACK_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$BlackNode";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeMap.RedNode}.
	 */
	public final static String STORAGE_TREE_MAP_RED_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$RedNode";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageSetView}.
	 */
	public final static String STORAGE_SET_VIEW_NAME = "io.takamaka.code.util.StorageSetView";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageMap}.
	 */
	public final static String STORAGE_MAP_NAME = "io.takamaka.code.util.StorageMap";
	
	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageArray}.
	 */
	public final static String STORAGE_ARRAY_NAME = "io.takamaka.code.util.StorageArray";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageListView}.
	 */
	public final static String STORAGE_LIST_VIEW_NAME = "io.takamaka.code.util.StorageListView";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageList}.
	 */
	public final static String STORAGE_LIST_NAME = "io.takamaka.code.util.StorageList";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static String STORAGE_TREE_MAP_NODE_NAME = "io.takamaka.code.util.StorageTreeMap$Node";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static String STORAGE_TREE_INTMAP_NODE_NAME = "io.takamaka.code.util.StorageTreeIntMap$Node";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static String STORAGE_LINKED_LIST_NAME = "io.takamaka.code.util.StorageLinkedList";

	/**
	 * The name of the class type for {@code io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	public final static String STORAGE_LINKED_LIST_NODE_NAME = "io.takamaka.code.util.StorageLinkedList$Node";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.ConsensusUpdate}.
	 */
	public final static String CONSENSUS_UPDATE_NAME = "io.takamaka.code.governance.ConsensusUpdate";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.GasPriceUpdate}.
	 */
	public final static String GAS_PRICE_UPDATE_NAME = "io.takamaka.code.governance.GasPriceUpdate";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.InflationUpdate}.
	 */
	public final static String INFLATION_UPDATE_NAME = "io.takamaka.code.governance.InflationUpdate";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.VerificationVersionUpdate}.
	 */
	public final static String VERIFICATION_VERSION_UPDATE_NAME = "io.takamaka.code.governance.VerificationVersionUpdate";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.ValidatorsUpdate}.
	 */
	public final static String VALIDATORS_UPDATE_NAME = "io.takamaka.code.governance.ValidatorsUpdate";

	/**
	 * The name of the package prefix of all Takamaka implementation classes.
	 */
	public final static String IO_TAKAMAKA_CODE_PACKAGE_NAME = "io.takamaka.code.";

	/**
	 * The name of the package of the Takamaka language classes.
	 */
	public final static String IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + "lang.";
	
	/**
	 * The name of the package of the Takamaka utility classes.
	 */
	public final static String IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + "util.";

	/**
	 * The name of the package of the Takamaka tokens classes.
	 */
	public final static String IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + "tokens.";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.Storage}.
	 */
	public final static String STORAGE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Storage";

	/**
	 * The name of the class type for {@code io.takamaka.code.math.UnsignedBigInteger}.
	 */
	public final static String UNSIGNED_BIG_INTEGER_NAME = IO_TAKAMAKA_CODE_PACKAGE_NAME + "math.UnsignedBigInteger";

	/**
	 * The name of the class type for {@code io.takamaka.code.tokens.ERC20}.
	 */
	public final static String ERC20_NAME = IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME + "ERC20";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.Manifest}.
	 */
	public final static String MANIFEST_NAME = "io.takamaka.code.governance.Manifest";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.Validator}.
	 */
	public final static String VALIDATOR_NAME = "io.takamaka.code.governance.Validator";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.Validators}.
	 */
	public final static String VALIDATORS_NAME = "io.takamaka.code.governance.Validators";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.Versions}.
	 */
	public final static String VERSIONS_NAME = "io.takamaka.code.governance.Versions";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.AccountsLedger}.
	 */
	public final static String ACCOUNTS_LEDGER_NAME = "io.takamaka.code.governance.AccountsLedger";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.GasStation}.
	 */
	public final static String GAS_STATION_NAME = "io.takamaka.code.governance.GasStation";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static String GENERIC_GAS_STATION_NAME = "io.takamaka.code.governance.GenericGasStation";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.tendermint.TendermintValidators}.
	 */
	public final static String TENDERMINT_VALIDATORS_NAME = "io.takamaka.code.governance.tendermint.TendermintValidators";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.tendermint.TendermintED25519Validator}.
	 */
	public final static String TENDERMINT_ED25519_VALIDATOR_NAME = "io.takamaka.code.governance.tendermint.TendermintED25519Validator";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.Payable}.
	 */
	public final static String PAYABLE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Payable";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.RedPayable}.
	 */
	public final static String RED_PAYABLE_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "RedPayable";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	public final static String FROM_CONTRACT_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "FromContract";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static String THROWS_EXCEPTIONS_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "ThrowsExceptions";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.SelfCharged}.
	 */
	public final static String SELF_CHARGED_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "SelfCharged";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.Event}.
	 */
	public final static String EVENT_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Event";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.View}.
	 */
	public final static String VIEW_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "View";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.Exported}.
	 */
	public final static String EXPORTED_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Exported";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.Takamaka}.
	 */
	public final static String TAKAMAKA_NAME = IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + "Takamaka";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.RequirementViolationException}.
	 */
	public final static String REQUIREMENT_VIOLATION_EXCEPTION_NAME = "io.takamaka.code.lang.RequirementViolationException";

	/**
	 * The name of the class type for {@code io.takamaka.code.lang.InsufficientFundsError}.
	 */
	public final static String INSUFFICIENT_FUNDS_ERROR_NAME = "io.takamaka.code.lang.InsufficientFundsError";

	/**
	 * The name of the class type for {@code io.takamaka.code.governance.GenericValidators}.
	 */
	public final static String GENERIC_VALIDATORS_NAME = "io.takamaka.code.governance.GenericValidators";
	
	/**
	 * The name of the class type for {@code io.takamaka.code.dao.Poll}.
	 */
	public final static String POLL_NAME = "io.takamaka.code.dao.Poll";
	
	/**
	 * The frequently used class type for {@code io.takamaka.code.dao.SharedEntity}.
	 */
	public final static String SHARED_ENTITY_NAME = "io.takamaka.code.dao.SharedEntity";

	/**
	 * The frequently used class type for {@code io.takamaka.code.dao.SharedEntity.Offer}.
	 */
	public final static String SHARED_ENTITY_OFFER_NAME = "io.takamaka.code.dao.SharedEntity$Offer";

	/**
	 * The frequently used class type for {@code io.takamaka.code.dao.SharedEntityView}.
	 */
	public final static String SHARED_ENTITY_VIEW_NAME = "io.takamaka.code.dao.SharedEntityView";
}