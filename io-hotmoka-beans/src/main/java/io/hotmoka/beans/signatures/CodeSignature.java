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

package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * The signature of a method or constructor.
 */
@Immutable
public abstract class CodeSignature extends AbstractMarshallable {

	/**
	 * The class of the method or constructor.
	 */
	public final ClassType definingClass;

	/**
	 * The formal arguments of the method or constructor.
	 */
	private final StorageType[] formals;

	/**
	 * The constructor ExternallyOwnedAccount(BigInteger, String).
	 */
	public final static ConstructorSignature EOA_CONSTRUCTOR = new ConstructorSignature(StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);

	/**
	 * The method {@code balance} of a contract.
	 */
	public final static MethodSignature BALANCE = new NonVoidMethodSignature(StorageTypes.CONTRACT, "balance", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code balanceRed} of a contract.
	 */
	public final static MethodSignature BALANCE_RED = new NonVoidMethodSignature(StorageTypes.CONTRACT, "balanceRed", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code publicKey} of an account.
	 */
	public final static MethodSignature PUBLIC_KEY = new NonVoidMethodSignature(StorageTypes.ACCOUNT, "publicKey", StorageTypes.STRING);

	/**
	 * The method {@code nonce} of an account.
	 */
	public final static MethodSignature NONCE = new NonVoidMethodSignature(StorageTypes.ACCOUNT, "nonce", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getGenesisTime} of the manifest.
	 */
	public final static MethodSignature GET_GENESIS_TIME = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getGenesisTime", StorageTypes.STRING);

	/**
	 * The method {@code getChainId} of the manifest.
	 */
	public final static MethodSignature GET_CHAIN_ID = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getChainId", StorageTypes.STRING);

	/**
	 * The method {@code getMaxErrorLength} of the manifest.
	 */
	public final static MethodSignature GET_MAX_ERROR_LENGTH = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getMaxErrorLength", StorageTypes.LONG);

	/**
	 * The method {@code getMaxDependencies} of the manifest.
	 */
	public final static MethodSignature GET_MAX_DEPENDENCIES = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getMaxDependencies", StorageTypes.LONG);

	/**
	 * The method {@code getMaxCumulativeSizeOfDependencies} of the manifest.
	 */
	public final static MethodSignature GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getMaxCumulativeSizeOfDependencies", StorageTypes.LONG);

	/**
	 * The method {@code getTicketForNewPoll} of the validators.
	 */
	public final static MethodSignature GET_TICKET_FOR_NEW_POLL = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getTicketForNewPoll", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getHeight} of the validators.
	 */
	public final static MethodSignature GET_HEIGHT = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getHeight", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getCurrentSupply} of the validators.
	 */
	public final static MethodSignature GET_CURRENT_SUPPLY = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getCurrentSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getNumberOfTransactions} of the validators.
	 */
	public final static MethodSignature GET_NUMBER_OF_TRANSACTIONS = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getNumberOfTransactions", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxFaucet} of the gamete.
	 */
	public final static MethodSignature GET_MAX_FAUCET = new NonVoidMethodSignature(StorageTypes.GAMETE, "getMaxFaucet", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxRedFaucet} of the gamete.
	 */
	public final static MethodSignature GET_MAX_RED_FAUCET = new NonVoidMethodSignature(StorageTypes.GAMETE, "getMaxRedFaucet", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code allowsSelfCharged} of the manifest.
	 */
	public final static MethodSignature ALLOWS_SELF_CHARGED = new NonVoidMethodSignature(StorageTypes.MANIFEST, "allowsSelfCharged", StorageTypes.BOOLEAN);

	/**
	 * The method {@code allowsUnsignedFaucet} of the manifest.
	 */
	public final static MethodSignature ALLOWS_UNSIGNED_FAUCET = new NonVoidMethodSignature(StorageTypes.MANIFEST, "allowsUnsignedFaucet", StorageTypes.BOOLEAN);

	/**
	 * The method {@code skipsVerification} of the manifest.
	 */
	public final static MethodSignature SKIPS_VERIFICATION = new NonVoidMethodSignature(StorageTypes.MANIFEST, "skipsVerification", StorageTypes.BOOLEAN);

	/**
	 * The method {@code getSignature} of the manifest.
	 */
	public final static MethodSignature GET_SIGNATURE = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getSignature", StorageTypes.STRING);

	/**
	 * The method {@code getGamete} of the manifest.
	 */
	public final static MethodSignature GET_GAMETE = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getGamete", StorageTypes.GAMETE);

	/**
	 * The method {@code getGasStation} of the manifest.
	 */
	public final static MethodSignature GET_GAS_STATION = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getGasStation", StorageTypes.GAS_STATION);

	/**
	 * The method {@code getVersions} of the manifest.
	 */
	public final static MethodSignature GET_VERSIONS = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getVersions", StorageTypes.VERSIONS);

	/**
	 * The method {@code getAccountsLedger} of the manifest.
	 */
	public final static MethodSignature GET_ACCOUNTS_LEDGER = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getAccountsLedger", StorageTypes.ACCOUNTS_LEDGER);

	/**
	 * The method {@code get} of the account ledger.
	 */
	public final static MethodSignature GET_FROM_ACCOUNTS_LEDGER = new NonVoidMethodSignature(StorageTypes.ACCOUNTS_LEDGER, "get", StorageTypes.EOA, StorageTypes.STRING);

	/**
	 * The method {@code getGasPrice} of the gas station.
	 */
	public final static MethodSignature GET_GAS_PRICE = new NonVoidMethodSignature(StorageTypes.GAS_STATION, "getGasPrice", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getMaxGasPerTransaction} of the gas station.
	 */
	public final static MethodSignature GET_MAX_GAS_PER_TRANSACTION = new NonVoidMethodSignature(StorageTypes.GAS_STATION, "getMaxGasPerTransaction", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getInitialGasPrice} of the gas station.
	 */
	public final static MethodSignature GET_INITIAL_GAS_PRICE = new NonVoidMethodSignature(StorageTypes.GAS_STATION, "getInitialGasPrice", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getTargetGasAtReward} of the gas station.
	 */
	public final static MethodSignature GET_TARGET_GAS_AT_REWARD = new NonVoidMethodSignature(StorageTypes.GAS_STATION, "getTargetGasAtReward", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getOblivion} of the gas station.
	 */
	public final static MethodSignature GET_OBLIVION = new NonVoidMethodSignature(StorageTypes.GAS_STATION, "getOblivion", StorageTypes.LONG);

	/**
	 * The method {@code getStake} of the validators object.
	 */
	public final static MethodSignature GET_STAKE = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getStake", StorageTypes.BIG_INTEGER, StorageTypes.VALIDATOR);

	/**
	 * The method {@code getInitialInflation} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_INFLATION = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getInitialInflation", StorageTypes.LONG);

	/**
	 * The method {@code getCurrentInflation} of the validators object.
	 */
	public final static MethodSignature GET_CURRENT_INFLATION = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getCurrentInflation", StorageTypes.LONG);

	/**
	 * The method {@code ignoresGasPrice} of the gas station.
	 */
	public final static MethodSignature IGNORES_GAS_PRICE = new NonVoidMethodSignature(StorageTypes.GAS_STATION, "ignoresGasPrice", StorageTypes.BOOLEAN);

	/**
	 * The method {@code getValidators} of the manifest.
	 */
	public final static MethodSignature GET_VALIDATORS = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getValidators", StorageTypes.VALIDATORS);

	/**
	 * The method {@code getInitialValidators} of the manifest.
	 */
	public final static MethodSignature GET_INITIAL_VALIDATORS = new NonVoidMethodSignature(StorageTypes.MANIFEST, "getInitialValidators", StorageTypes.SHARED_ENTITY_VIEW);

	/**
	 * The method {@code getVerificationVersion} of the versions object.
	 */
	public final static MethodSignature GET_VERIFICATION_VERSION = new NonVoidMethodSignature(StorageTypes.VERSIONS, "getVerificationVersion", StorageTypes.LONG);

	/**
	 * The method {@code getPolls} of the validators object.
	 */
	public final static MethodSignature GET_POLLS = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getPolls", StorageTypes.STORAGE_SET_VIEW);

	/**
	 * The method {@code getInitialSupply} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_SUPPLY = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getInitialSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getInitialRedSupply} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_RED_SUPPLY = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getInitialRedSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code getFinalSupply} of the validators object.
	 */
	public final static MethodSignature GET_FINAL_SUPPLY = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getFinalSupply", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code add} of the account ledger.
	 */
	public final static MethodSignature ADD_INTO_ACCOUNTS_LEDGER = new NonVoidMethodSignature(StorageTypes.ACCOUNTS_LEDGER, "add", StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);

	/**
	 * The method {@code id} of a validator.
	 */
	public final static MethodSignature ID = new NonVoidMethodSignature(StorageTypes.VALIDATOR, "id", StorageTypes.STRING);

	/**
	 * The method {@code receive} of a payable contract, with a big integer argument.
	 */
	public final static MethodSignature RECEIVE_BIG_INTEGER = new VoidMethodSignature(StorageTypes.PAYABLE_CONTRACT, "receive", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code receiveRed} of a payable contract, with a big integer argument.
	 */
	public final static MethodSignature RECEIVE_RED_BIG_INTEGER = new VoidMethodSignature(StorageTypes.PAYABLE_CONTRACT, "receiveRed", StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code receive} of a payable contract, with an int argument.
	 */
	public final static MethodSignature RECEIVE_INT = new VoidMethodSignature(StorageTypes.PAYABLE_CONTRACT, "receive", StorageTypes.INT);

	/**
	 * The method {@code receive} of a payable contract, with a long argument.
	 */
	public final static MethodSignature RECEIVE_LONG = new VoidMethodSignature(StorageTypes.PAYABLE_CONTRACT, "receive", StorageTypes.LONG);

	/**
	 * The method {@code reward} of the validators contract.
	 */
	public final static MethodSignature VALIDATORS_REWARD = new VoidMethodSignature
		(StorageTypes.VALIDATORS, "reward", StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.STRING, StorageTypes.STRING, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER);

	/**
	 * The method {@code newPoll} of the generic validators contract.
	 */
	public final static MethodSignature NEW_POLL = new NonVoidMethodSignature(StorageTypes.GENERIC_VALIDATORS, "newPoll", StorageTypes.POLL);
	
	/**
	 * The method {@code newPollWithTimeParams} of the generic validators contract with time parameters.
	 */
	public final static MethodSignature NEW_POLL_WITH_TIME_PARAMS = new NonVoidMethodSignature(StorageTypes.GENERIC_VALIDATORS, "newPollWithTimeParams", StorageTypes.POLL, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER);
	
	/**
	 * The method {@code isVoteOver} of the Poll contract.
	 */
	public final static MethodSignature IS_VOTE_OVER = new NonVoidMethodSignature(StorageTypes.POLL, "isVoteOver", StorageTypes.BOOLEAN);
	
	/**
	 * The method {@code closePoll} of the Poll contract.
	 */
	public final static MethodSignature CLOSE_POLL = new VoidMethodSignature(StorageTypes.POLL, "closePoll");
	
	/**
	 * The method {@code vote} of the Poll contract.
	 */
	public final static MethodSignature VOTE = new VoidMethodSignature(StorageTypes.POLL, "vote");
	
	/**
	 * The method {@code vote} of the Poll contract with the share parameter.
	 */
	public final static MethodSignature VOTE_WITH_SHARE = new VoidMethodSignature(StorageTypes.POLL, "vote", StorageTypes.BIG_INTEGER);
	
	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	protected CodeSignature(ClassType definingClass, StorageType... formals) {
		this.definingClass = Objects.requireNonNull(definingClass, "definingClass cannot be null");
		this.formals = Objects.requireNonNull(formals, "formals cannot be null");
		Stream.of(formals).forEach(formal -> Objects.requireNonNull(formal, "formals cannot hold null"));
	}

	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the name of the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	public CodeSignature(String definingClass, StorageType... formals) {
		this(StorageTypes.classNamed(definingClass), formals);
	}

	/**
	 * Yields the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the formal arguments
	 */
	public final Stream<StorageType> formals() {
		return Stream.of(formals);
	}

	/**
	 * Yields a comma-separated string of the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the string
	 */
	protected final String commaSeparatedFormals() {
		return formals()
			.map(StorageType::toString)
			.collect(Collectors.joining(",", "(", ")"));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CodeSignature cs && cs.definingClass.equals(definingClass) && Arrays.equals(cs.formals, formals);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ Arrays.hashCode(formals);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		definingClass.into(context);
		context.writeLengthAndArray(formals);
	}

	/**
	 * Factory method that unmarshals a code signature from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the code signature
	 * @throws IOException if the code signature cannot be unmarshalled
	 */
	public static CodeSignature from(UnmarshallingContext context) throws IOException {
		var selector = context.readByte();
		if (selector == ConstructorSignature.SELECTOR_EOA)
			return ConstructorSignature.EOA_CONSTRUCTOR;
		else if (selector == VoidMethodSignature.SELECTOR_REWARD)
			return VoidMethodSignature.VALIDATORS_REWARD;

		ClassType definingClass;

		try {
			definingClass = (ClassType) StorageTypes.from(context);
		}
		catch (ClassCastException e) {
			throw new IOException("Failed to unmarshal a code signature", e);
		}

		var formals = context.readLengthAndArray(StorageTypes::from, StorageType[]::new);

		switch (selector) {
		case ConstructorSignature.SELECTOR: return new ConstructorSignature(definingClass, formals);
		case VoidMethodSignature.SELECTOR: return new VoidMethodSignature(definingClass, context.readStringUnshared(), formals);
		case NonVoidMethodSignature.SELECTOR: return new NonVoidMethodSignature(definingClass, context.readStringUnshared(), StorageTypes.from(context), formals);
		default: throw new IOException("Unexpected code signature selector: " + selector);
		}
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}