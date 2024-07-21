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

package io.hotmoka.node.internal.signatures;

import java.io.IOException;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;

/**
 * The signature of a method of a class.
 */
@Immutable
public abstract class AbstractMethodSignature extends AbstractCodeSignature implements MethodSignature {

	/**
	 * The name of the method.
	 */
	private final String methodName;

	/**
	 * Builds the signature of a method.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	protected AbstractMethodSignature(ClassType definingClass, String methodName, StorageType... formals) {
		super(definingClass, formals);

		this.methodName = Objects.requireNonNull(methodName, "methodName cannot be null");
	}

	@Override
	public final String getMethodName() {
		return methodName;
	}

	@Override
	public String toString() {
		return getDefiningClass() + "." + methodName + commaSeparatedFormals();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof MethodSignature ms && methodName.equals(ms.getMethodName()) && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ methodName.hashCode();
	}

	/**
	 * Factory method that unmarshals a method signature from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the method signature
	 * @throws IOException if the method signature cannot be unmarshalled
	 */
	public static MethodSignature from(UnmarshallingContext context) throws IOException {
		ClassType definingClass;

		try {
			definingClass = (ClassType) StorageTypes.from(context);
		}
		catch (ClassCastException e) {
			throw new IOException("Failed to unmarshal a code signature", e);
		}

		var methodName = context.readStringUnshared();

		int length = context.readCompactInt();

		// we determine if the method is void or not, by looking at the parity of the number of formals
		// (see the into() method in NonVoidMethodSignatureImpl and VoidMethodSignatureImpl)
		boolean isVoid = length % 2 == 0;
		length /= 2;

		var formals = new StorageType[length];
		for (int pos = 0; pos < length; pos++)
			formals[pos] = StorageTypes.from(context);

		if (isVoid)
			return MethodSignatures.ofVoid(definingClass, methodName, formals);
		else
			return MethodSignatures.ofNonVoid(definingClass, methodName, StorageTypes.from(context), formals);
	}

	/**
	 * The method {@code balance} of a contract.
	 */
	public final static NonVoidMethodSignature BALANCE = MethodSignatures.ofNonVoid(StorageTypes.CONTRACT, "balance", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code balanceRed} of a contract.
	 */
	public final static NonVoidMethodSignature BALANCE_RED = MethodSignatures.ofNonVoid(StorageTypes.CONTRACT, "balanceRed", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code publicKey} of an account.
	 */
	public final static NonVoidMethodSignature PUBLIC_KEY = MethodSignatures.ofNonVoid(StorageTypes.ACCOUNT, "publicKey", StorageTypes.STRING);

	/**
	 * The method {@code nonce} of an account.
	 */
	public final static NonVoidMethodSignature NONCE = MethodSignatures.ofNonVoid(StorageTypes.ACCOUNT, "nonce", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getGenesisTime} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GENESIS_TIME = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getGenesisTime", StorageTypes.STRING);

	/**
	 * The method {@code getChainId} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_CHAIN_ID = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getChainId", StorageTypes.STRING);

	/**
	 * The method {@code getMaxErrorLength} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_ERROR_LENGTH = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getMaxErrorLength", StorageTypes.INT);

	/**
	 * The method {@code getMaxDependencies} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_DEPENDENCIES = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getMaxDependencies", StorageTypes.INT);

	/**
	 * The method {@code getMaxCumulativeSizeOfDependencies} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getMaxCumulativeSizeOfDependencies", StorageTypes.LONG);

	/**
	 * The method {@code getTicketForNewPoll} of the validators.
	 */
	public final static NonVoidMethodSignature GET_TICKET_FOR_NEW_POLL = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getTicketForNewPoll", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getHeight} of the validators.
	 */
	public final static NonVoidMethodSignature GET_HEIGHT = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getHeight", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getCurrentSupply} of the validators.
	 */
	public final static NonVoidMethodSignature GET_CURRENT_SUPPLY = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getCurrentSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getNumberOfTransactions} of the validators.
	 */
	public final static NonVoidMethodSignature GET_NUMBER_OF_TRANSACTIONS = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getNumberOfTransactions", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxFaucet} of the gamete.
	 */
	public final static NonVoidMethodSignature GET_MAX_FAUCET = MethodSignatures.ofNonVoid(StorageTypes.GAMETE, "getMaxFaucet", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxRedFaucet} of the gamete.
	 */
	public final static NonVoidMethodSignature GET_MAX_RED_FAUCET = MethodSignatures.ofNonVoid(StorageTypes.GAMETE, "getMaxRedFaucet", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code allowsUnsignedFaucet} of the manifest.
	 */
	public final static NonVoidMethodSignature ALLOWS_UNSIGNED_FAUCET = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "allowsUnsignedFaucet", StorageTypes.BOOLEAN);

	/**
	 * The method {@code skipsVerification} of the manifest.
	 */
	public final static NonVoidMethodSignature SKIPS_VERIFICATION = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "skipsVerification", StorageTypes.BOOLEAN);

	/**
	 * The method {@code getSignature} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_SIGNATURE = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getSignature", StorageTypes.STRING);

	/**
	 * The method {@code getGamete} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GAMETE = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getGamete", StorageTypes.GAMETE);

	/**
	 * The method {@code getGasStation} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GAS_STATION = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getGasStation", StorageTypes.GAS_STATION);

	/**
	 * The method {@code getVersions} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_VERSIONS = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getVersions", StorageTypes.VERSIONS);

	/**
	 * The method {@code getAccountsLedger} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_ACCOUNTS_LEDGER = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getAccountsLedger", StorageTypes.ACCOUNTS_LEDGER);

	/**
	 * The method {@code get} of the account ledger.
	 */
	public final static NonVoidMethodSignature GET_FROM_ACCOUNTS_LEDGER = MethodSignatures.ofNonVoid(StorageTypes.ACCOUNTS_LEDGER, "get", StorageTypes.EOA, StorageTypes.STRING);

	/**
	 * The method {@code getGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_GAS_PRICE = MethodSignatures.ofNonVoid(StorageTypes.GAS_STATION, "getGasPrice", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxGasPerTransaction} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_MAX_GAS_PER_TRANSACTION = MethodSignatures.ofNonVoid(StorageTypes.GAS_STATION, "getMaxGasPerTransaction", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getInitialGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_GAS_PRICE = MethodSignatures.ofNonVoid(StorageTypes.GAS_STATION, "getInitialGasPrice", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getTargetGasAtReward} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_TARGET_GAS_AT_REWARD = MethodSignatures.ofNonVoid(StorageTypes.GAS_STATION, "getTargetGasAtReward", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getOblivion} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_OBLIVION = MethodSignatures.ofNonVoid(StorageTypes.GAS_STATION, "getOblivion", StorageTypes.LONG);

	/**
	 * The method {@code getStake} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_STAKE = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getStake", StorageTypes.BIG_INTEGER, StorageTypes.VALIDATOR);

	/**
	 * The method {@code getInitialInflation} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_INFLATION = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getInitialInflation", StorageTypes.LONG);

	/**
	 * The method {@code getCurrentInflation} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_CURRENT_INFLATION = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getCurrentInflation", StorageTypes.LONG);

	/**
	 * The method {@code getShares} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_SHARES = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getShares", StorageTypes.STORAGE_MAP_VIEW);

	/**
	 * The method {@code ignoresGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature IGNORES_GAS_PRICE = MethodSignatures.ofNonVoid(StorageTypes.GAS_STATION, "ignoresGasPrice", StorageTypes.BOOLEAN);

	/**
	 * The method {@code getValidators} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_VALIDATORS = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getValidators", StorageTypes.VALIDATORS);

	/**
	 * The method {@code getInitialValidators} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_VALIDATORS = MethodSignatures.ofNonVoid(StorageTypes.MANIFEST, "getInitialValidators", StorageTypes.SHARED_ENTITY_VIEW);

	/**
	 * The method {@code getVerificationVersion} of the versions object.
	 */
	public final static NonVoidMethodSignature GET_VERIFICATION_VERSION = MethodSignatures.ofNonVoid(StorageTypes.VERSIONS, "getVerificationVersion", StorageTypes.LONG);

	/**
	 * The method {@code getPolls} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_POLLS = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getPolls", StorageTypes.STORAGE_SET_VIEW);

	/**
	 * The method {@code getInitialSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_SUPPLY = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getInitialSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getInitialRedSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_RED_SUPPLY = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getInitialRedSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getFinalSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_FINAL_SUPPLY = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getFinalSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code add} of the account ledger.
	 */
	public final static NonVoidMethodSignature ADD_INTO_ACCOUNTS_LEDGER = MethodSignatures.ofNonVoid(StorageTypes.ACCOUNTS_LEDGER, "add", StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);

	/**
	 * The method {@code id} of a validator.
	 */
	public final static NonVoidMethodSignature ID = MethodSignatures.ofNonVoid(StorageTypes.VALIDATOR, "id", StorageTypes.STRING);

	/**
	 * The method {@code receive} of a payable contract, with a big integer argument.
	 */
	public final static VoidMethodSignature RECEIVE_BIG_INTEGER = MethodSignatures.ofVoid(StorageTypes.PAYABLE_CONTRACT, "receive", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code receiveRed} of a payable contract, with a big integer argument.
	 */
	public final static VoidMethodSignature RECEIVE_RED_BIG_INTEGER = MethodSignatures.ofVoid(StorageTypes.PAYABLE_CONTRACT, "receiveRed", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code receive} of a payable contract, with an {@code int} argument.
	 */
	public final static VoidMethodSignature RECEIVE_INT = MethodSignatures.ofVoid(StorageTypes.PAYABLE_CONTRACT, "receive", StorageTypes.INT);

	/**
	 * The method {@code receive} of a payable contract, with a {@code long} argument.
	 */
	public final static VoidMethodSignature RECEIVE_LONG = MethodSignatures.ofVoid(StorageTypes.PAYABLE_CONTRACT, "receive", StorageTypes.LONG);

	/**
	 * The method {@code reward} of the validators contract.
	 */
	public final static VoidMethodSignature VALIDATORS_REWARD = MethodSignatures.ofVoid
		(StorageTypes.VALIDATORS, "reward", StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.STRING, StorageTypes.STRING, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code rewardMokamintNode} of the validators contract.
	 */
	public final static VoidMethodSignature VALIDATORS_REWARD_MOKAMINT_NODE = MethodSignatures.ofVoid
		(StorageTypes.VALIDATORS, "rewardMokamintNode", StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.STRING, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code rewardMokamintMiner} of the validators contract.
	 */
	public final static VoidMethodSignature VALIDATORS_REWARD_MOKAMINT_MINER = MethodSignatures.ofVoid(StorageTypes.VALIDATORS, "rewardMokamintMiner", StorageTypes.BIG_INTEGER, StorageTypes.STRING);

	/**
	 * The method {@code getBuyerSurcharge} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_BUYER_SURCHARGE = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getBuyerSurcharge", StorageTypes.INT);

	/**
	 * The method {@code getSlashingForMisbehaving} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_SLASHING_FOR_MISBEHAVING = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getSlashingForMisbehaving", StorageTypes.INT);

	/**
	 * The method {@code getSlashingForNotBehaving} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getSlashingForNotBehaving", StorageTypes.INT);

	/**
	 * The method {@code getPercentStaked} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_PERCENT_STAKED = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getPercentStaked", StorageTypes.INT);
	
	/**
	 * The method {@code newPoll} of the generic validators contract.
	 */
	public final static NonVoidMethodSignature NEW_POLL = MethodSignatures.ofNonVoid(StorageTypes.GENERIC_VALIDATORS, "newPoll", StorageTypes.POLL);
	
	/**
	 * The method {@code newPollWithTimeParams} of the generic validators contract with time parameters.
	 */
	public final static NonVoidMethodSignature NEW_POLL_WITH_TIME_PARAMS = MethodSignatures.ofNonVoid(StorageTypes.GENERIC_VALIDATORS, "newPollWithTimeParams", StorageTypes.POLL, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER);
	
	/**
	 * The method {@code isVoteOver} of the Poll contract.
	 */
	public final static NonVoidMethodSignature IS_VOTE_OVER = MethodSignatures.ofNonVoid(StorageTypes.POLL, "isVoteOver", StorageTypes.BOOLEAN);
	
	/**
	 * The method {@code closePoll} of the Poll contract.
	 */
	public final static VoidMethodSignature CLOSE_POLL = MethodSignatures.ofVoid(StorageTypes.POLL, "closePoll");
	
	/**
	 * The method {@code vote} of the Poll contract.
	 */
	public final static VoidMethodSignature VOTE = MethodSignatures.ofVoid(StorageTypes.POLL, "vote");
	
	/**
	 * The method {@code vote} of the Poll contract with the share parameter.
	 */
	public final static VoidMethodSignature VOTE_WITH_SHARE = MethodSignatures.ofVoid(StorageTypes.POLL, "vote", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code size} of a storage map view contract.
	 */
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_SIZE = MethodSignatures.ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "size", StorageTypes.INT);

	/**
	 * The method {@code select} of a {@code StorageMapView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_SELECT = MethodSignatures.ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "select", StorageTypes.OBJECT, StorageTypes.INT);

	/**
	 * The method {@code get} of a {@code StorageMapView} contract.
	 */	
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_GET = MethodSignatures.ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "get", StorageTypes.OBJECT, StorageTypes.OBJECT);

	/**
	 * The method {@code select} of a {@code StorageSetView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_SET_VIEW_SELECT = MethodSignatures.ofNonVoid(StorageTypes.STORAGE_SET_VIEW, "select", StorageTypes.OBJECT, StorageTypes.INT);

	/**
	 * The method {@code size} of a {@code StorageSetView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_SET_VIEW_SIZE = MethodSignatures.ofNonVoid(StorageTypes.STORAGE_SET_VIEW, "size", StorageTypes.INT);
}