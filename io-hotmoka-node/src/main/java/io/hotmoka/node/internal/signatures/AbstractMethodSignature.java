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
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.signatures.MethodSignature;
import io.hotmoka.beans.api.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.api.signatures.VoidMethodSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.MethodSignatures;

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
			return MethodSignatures.of(definingClass, methodName, StorageTypes.from(context), formals);
	}

	/**
	 * The method {@code balance} of a contract.
	 */
	public final static NonVoidMethodSignature BALANCE = MethodSignatures.of(StorageTypes.CONTRACT, "balance", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code balanceRed} of a contract.
	 */
	public final static NonVoidMethodSignature BALANCE_RED = MethodSignatures.of(StorageTypes.CONTRACT, "balanceRed", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code publicKey} of an account.
	 */
	public final static NonVoidMethodSignature PUBLIC_KEY = MethodSignatures.of(StorageTypes.ACCOUNT, "publicKey", StorageTypes.STRING);

	/**
	 * The method {@code nonce} of an account.
	 */
	public final static NonVoidMethodSignature NONCE = MethodSignatures.of(StorageTypes.ACCOUNT, "nonce", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getGenesisTime} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GENESIS_TIME = MethodSignatures.of(StorageTypes.MANIFEST, "getGenesisTime", StorageTypes.STRING);

	/**
	 * The method {@code getChainId} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_CHAIN_ID = MethodSignatures.of(StorageTypes.MANIFEST, "getChainId", StorageTypes.STRING);

	/**
	 * The method {@code getMaxErrorLength} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_ERROR_LENGTH = MethodSignatures.of(StorageTypes.MANIFEST, "getMaxErrorLength", StorageTypes.LONG);

	/**
	 * The method {@code getMaxDependencies} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_DEPENDENCIES = MethodSignatures.of(StorageTypes.MANIFEST, "getMaxDependencies", StorageTypes.LONG);

	/**
	 * The method {@code getMaxCumulativeSizeOfDependencies} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES = MethodSignatures.of(StorageTypes.MANIFEST, "getMaxCumulativeSizeOfDependencies", StorageTypes.LONG);

	/**
	 * The method {@code getTicketForNewPoll} of the validators.
	 */
	public final static NonVoidMethodSignature GET_TICKET_FOR_NEW_POLL = MethodSignatures.of(StorageTypes.VALIDATORS, "getTicketForNewPoll", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getHeight} of the validators.
	 */
	public final static NonVoidMethodSignature GET_HEIGHT = MethodSignatures.of(StorageTypes.VALIDATORS, "getHeight", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getCurrentSupply} of the validators.
	 */
	public final static NonVoidMethodSignature GET_CURRENT_SUPPLY = MethodSignatures.of(StorageTypes.VALIDATORS, "getCurrentSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getNumberOfTransactions} of the validators.
	 */
	public final static NonVoidMethodSignature GET_NUMBER_OF_TRANSACTIONS = MethodSignatures.of(StorageTypes.VALIDATORS, "getNumberOfTransactions", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxFaucet} of the gamete.
	 */
	public final static NonVoidMethodSignature GET_MAX_FAUCET = MethodSignatures.of(StorageTypes.GAMETE, "getMaxFaucet", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxRedFaucet} of the gamete.
	 */
	public final static NonVoidMethodSignature GET_MAX_RED_FAUCET = MethodSignatures.of(StorageTypes.GAMETE, "getMaxRedFaucet", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code allowsUnsignedFaucet} of the manifest.
	 */
	public final static NonVoidMethodSignature ALLOWS_UNSIGNED_FAUCET = MethodSignatures.of(StorageTypes.MANIFEST, "allowsUnsignedFaucet", StorageTypes.BOOLEAN);

	/**
	 * The method {@code skipsVerification} of the manifest.
	 */
	public final static NonVoidMethodSignature SKIPS_VERIFICATION = MethodSignatures.of(StorageTypes.MANIFEST, "skipsVerification", StorageTypes.BOOLEAN);

	/**
	 * The method {@code getSignature} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_SIGNATURE = MethodSignatures.of(StorageTypes.MANIFEST, "getSignature", StorageTypes.STRING);

	/**
	 * The method {@code getGamete} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GAMETE = MethodSignatures.of(StorageTypes.MANIFEST, "getGamete", StorageTypes.GAMETE);

	/**
	 * The method {@code getGasStation} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GAS_STATION = MethodSignatures.of(StorageTypes.MANIFEST, "getGasStation", StorageTypes.GAS_STATION);

	/**
	 * The method {@code getVersions} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_VERSIONS = MethodSignatures.of(StorageTypes.MANIFEST, "getVersions", StorageTypes.VERSIONS);

	/**
	 * The method {@code getAccountsLedger} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_ACCOUNTS_LEDGER = MethodSignatures.of(StorageTypes.MANIFEST, "getAccountsLedger", StorageTypes.ACCOUNTS_LEDGER);

	/**
	 * The method {@code get} of the account ledger.
	 */
	public final static NonVoidMethodSignature GET_FROM_ACCOUNTS_LEDGER = MethodSignatures.of(StorageTypes.ACCOUNTS_LEDGER, "get", StorageTypes.EOA, StorageTypes.STRING);

	/**
	 * The method {@code getGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_GAS_PRICE = MethodSignatures.of(StorageTypes.GAS_STATION, "getGasPrice", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxGasPerTransaction} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_MAX_GAS_PER_TRANSACTION = MethodSignatures.of(StorageTypes.GAS_STATION, "getMaxGasPerTransaction", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getInitialGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_GAS_PRICE = MethodSignatures.of(StorageTypes.GAS_STATION, "getInitialGasPrice", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getTargetGasAtReward} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_TARGET_GAS_AT_REWARD = MethodSignatures.of(StorageTypes.GAS_STATION, "getTargetGasAtReward", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getOblivion} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_OBLIVION = MethodSignatures.of(StorageTypes.GAS_STATION, "getOblivion", StorageTypes.LONG);

	/**
	 * The method {@code getStake} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_STAKE = MethodSignatures.of(StorageTypes.VALIDATORS, "getStake", StorageTypes.BIG_INTEGER, StorageTypes.VALIDATOR);

	/**
	 * The method {@code getInitialInflation} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_INFLATION = MethodSignatures.of(StorageTypes.VALIDATORS, "getInitialInflation", StorageTypes.LONG);

	/**
	 * The method {@code getCurrentInflation} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_CURRENT_INFLATION = MethodSignatures.of(StorageTypes.VALIDATORS, "getCurrentInflation", StorageTypes.LONG);

	/**
	 * The method {@code ignoresGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature IGNORES_GAS_PRICE = MethodSignatures.of(StorageTypes.GAS_STATION, "ignoresGasPrice", StorageTypes.BOOLEAN);

	/**
	 * The method {@code getValidators} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_VALIDATORS = MethodSignatures.of(StorageTypes.MANIFEST, "getValidators", StorageTypes.VALIDATORS);

	/**
	 * The method {@code getInitialValidators} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_VALIDATORS = MethodSignatures.of(StorageTypes.MANIFEST, "getInitialValidators", StorageTypes.SHARED_ENTITY_VIEW);

	/**
	 * The method {@code getVerificationVersion} of the versions object.
	 */
	public final static NonVoidMethodSignature GET_VERIFICATION_VERSION = MethodSignatures.of(StorageTypes.VERSIONS, "getVerificationVersion", StorageTypes.LONG);

	/**
	 * The method {@code getPolls} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_POLLS = MethodSignatures.of(StorageTypes.VALIDATORS, "getPolls", StorageTypes.STORAGE_SET_VIEW);

	/**
	 * The method {@code getInitialSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_SUPPLY = MethodSignatures.of(StorageTypes.VALIDATORS, "getInitialSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getInitialRedSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_RED_SUPPLY = MethodSignatures.of(StorageTypes.VALIDATORS, "getInitialRedSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getFinalSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_FINAL_SUPPLY = MethodSignatures.of(StorageTypes.VALIDATORS, "getFinalSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code add} of the account ledger.
	 */
	public final static NonVoidMethodSignature ADD_INTO_ACCOUNTS_LEDGER = MethodSignatures.of(StorageTypes.ACCOUNTS_LEDGER, "add", StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);

	/**
	 * The method {@code id} of a validator.
	 */
	public final static NonVoidMethodSignature ID = MethodSignatures.of(StorageTypes.VALIDATOR, "id", StorageTypes.STRING);

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
	 * The method {@code newPoll} of the generic validators contract.
	 */
	public final static NonVoidMethodSignature NEW_POLL = MethodSignatures.of(StorageTypes.GENERIC_VALIDATORS, "newPoll", StorageTypes.POLL);
	
	/**
	 * The method {@code newPollWithTimeParams} of the generic validators contract with time parameters.
	 */
	public final static NonVoidMethodSignature NEW_POLL_WITH_TIME_PARAMS = MethodSignatures.of(StorageTypes.GENERIC_VALIDATORS, "newPollWithTimeParams", StorageTypes.POLL, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER);
	
	/**
	 * The method {@code isVoteOver} of the Poll contract.
	 */
	public final static NonVoidMethodSignature IS_VOTE_OVER = MethodSignatures.of(StorageTypes.POLL, "isVoteOver", StorageTypes.BOOLEAN);
	
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
}