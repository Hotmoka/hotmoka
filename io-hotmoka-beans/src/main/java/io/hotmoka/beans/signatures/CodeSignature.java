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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.marshalling.MarshallableBean;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.marshalling.MarshallingContext;
import io.hotmoka.marshalling.UnmarshallingContext;

/**
 * The signature of a method or constructor.
 */
@Immutable
public abstract class CodeSignature extends MarshallableBean {

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
	public final static ConstructorSignature EOA_CONSTRUCTOR = new ConstructorSignature(ClassType.EOA, ClassType.BIG_INTEGER, ClassType.STRING);

	/**
	 * The method {@code balance} of a contract.
	 */
	public final static MethodSignature BALANCE = new NonVoidMethodSignature(ClassType.CONTRACT, "balance", ClassType.BIG_INTEGER);

	/**
	 * The method {@code balanceRed} of a contract.
	 */
	public final static MethodSignature BALANCE_RED = new NonVoidMethodSignature(ClassType.CONTRACT, "balanceRed", ClassType.BIG_INTEGER);

	/**
	 * The method {@code publicKey} of an account.
	 */
	public final static MethodSignature PUBLIC_KEY = new NonVoidMethodSignature(ClassType.ACCOUNT, "publicKey", ClassType.STRING);

	/**
	 * The method {@code nonce} of an account.
	 */
	public final static MethodSignature NONCE = new NonVoidMethodSignature(ClassType.ACCOUNT, "nonce", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getGenesisTime} of the manifest.
	 */
	public final static MethodSignature GET_GENESIS_TIME = new NonVoidMethodSignature(ClassType.MANIFEST, "getGenesisTime", ClassType.STRING);

	/**
	 * The method {@code getChainId} of the manifest.
	 */
	public final static MethodSignature GET_CHAIN_ID = new NonVoidMethodSignature(ClassType.MANIFEST, "getChainId", ClassType.STRING);

	/**
	 * The method {@code getMaxErrorLength} of the manifest.
	 */
	public final static MethodSignature GET_MAX_ERROR_LENGTH = new NonVoidMethodSignature(ClassType.MANIFEST, "getMaxErrorLength", BasicTypes.INT);

	/**
	 * The method {@code getMaxDependencies} of the manifest.
	 */
	public final static MethodSignature GET_MAX_DEPENDENCIES = new NonVoidMethodSignature(ClassType.MANIFEST, "getMaxDependencies", BasicTypes.INT);

	/**
	 * The method {@code getMaxCumulativeSizeOfDependencies} of the manifest.
	 */
	public final static MethodSignature GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES = new NonVoidMethodSignature(ClassType.MANIFEST, "getMaxCumulativeSizeOfDependencies", BasicTypes.LONG);

	/**
	 * The method {@code getTicketForNewPoll} of the validators.
	 */
	public final static MethodSignature GET_TICKET_FOR_NEW_POLL = new NonVoidMethodSignature(ClassType.VALIDATORS, "getTicketForNewPoll", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getHeight} of the validators.
	 */
	public final static MethodSignature GET_HEIGHT = new NonVoidMethodSignature(ClassType.VALIDATORS, "getHeight", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getCurrentSupply} of the validators.
	 */
	public final static MethodSignature GET_CURRENT_SUPPLY = new NonVoidMethodSignature(ClassType.VALIDATORS, "getCurrentSupply", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getNumberOfTransactions} of the validators.
	 */
	public final static MethodSignature GET_NUMBER_OF_TRANSACTIONS = new NonVoidMethodSignature(ClassType.VALIDATORS, "getNumberOfTransactions", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getMaxFaucet} of the gamete.
	 */
	public final static MethodSignature GET_MAX_FAUCET = new NonVoidMethodSignature(ClassType.GAMETE, "getMaxFaucet", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getMaxRedFaucet} of the gamete.
	 */
	public final static MethodSignature GET_MAX_RED_FAUCET = new NonVoidMethodSignature(ClassType.GAMETE, "getMaxRedFaucet", ClassType.BIG_INTEGER);

	/**
	 * The method {@code allowsSelfCharged} of the manifest.
	 */
	public final static MethodSignature ALLOWS_SELF_CHARGED = new NonVoidMethodSignature(ClassType.MANIFEST, "allowsSelfCharged", BasicTypes.BOOLEAN);

	/**
	 * The method {@code allowsUnsignedFaucet} of the manifest.
	 */
	public final static MethodSignature ALLOWS_UNSIGNED_FAUCET = new NonVoidMethodSignature(ClassType.MANIFEST, "allowsUnsignedFaucet", BasicTypes.BOOLEAN);

	/**
	 * The method {@code allowsMintBurnFromGamete} of the manifest.
	 */
	public final static MethodSignature ALLOWS_MINT_BURN_FROM_GAMETE = new NonVoidMethodSignature(ClassType.MANIFEST, "allowsMintBurnFromGamete", BasicTypes.BOOLEAN);

	/**
	 * The method {@code skipsVerification} of the manifest.
	 */
	public final static MethodSignature SKIPS_VERIFICATION = new NonVoidMethodSignature(ClassType.MANIFEST, "skipsVerification", BasicTypes.BOOLEAN);

	/**
	 * The method {@code getSignature} of the manifest.
	 */
	public final static MethodSignature GET_SIGNATURE = new NonVoidMethodSignature(ClassType.MANIFEST, "getSignature", ClassType.STRING);

	/**
	 * The method {@code getGamete} of the manifest.
	 */
	public final static MethodSignature GET_GAMETE = new NonVoidMethodSignature(ClassType.MANIFEST, "getGamete", ClassType.GAMETE);

	/**
	 * The method {@code getGasStation} of the manifest.
	 */
	public final static MethodSignature GET_GAS_STATION = new NonVoidMethodSignature(ClassType.MANIFEST, "getGasStation", ClassType.GAS_STATION);

	/**
	 * The method {@code getVersions} of the manifest.
	 */
	public final static MethodSignature GET_VERSIONS = new NonVoidMethodSignature(ClassType.MANIFEST, "getVersions", ClassType.VERSIONS);

	/**
	 * The method {@code getAccountsLedger} of the manifest.
	 */
	public final static MethodSignature GET_ACCOUNTS_LEDGER = new NonVoidMethodSignature(ClassType.MANIFEST, "getAccountsLedger", ClassType.ACCOUNTS_LEDGER);

	/**
	 * The method {@code get} of the account ledger.
	 */
	public final static MethodSignature GET_FROM_ACCOUNTS_LEDGER = new NonVoidMethodSignature(ClassType.ACCOUNTS_LEDGER, "get", ClassType.EOA, ClassType.STRING);

	/**
	 * The method {@code getGasPrice} of the gas station.
	 */
	public final static MethodSignature GET_GAS_PRICE = new NonVoidMethodSignature(ClassType.GAS_STATION, "getGasPrice", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getMaxGasPerTransaction} of the gas station.
	 */
	public final static MethodSignature GET_MAX_GAS_PER_TRANSACTION = new NonVoidMethodSignature(ClassType.GAS_STATION, "getMaxGasPerTransaction", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getInitialGasPrice} of the gas station.
	 */
	public final static MethodSignature GET_INITIAL_GAS_PRICE = new NonVoidMethodSignature(ClassType.GAS_STATION, "getInitialGasPrice", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getTargetGasAtReward} of the gas station.
	 */
	public final static MethodSignature GET_TARGET_GAS_AT_REWARD = new NonVoidMethodSignature(ClassType.GAS_STATION, "getTargetGasAtReward", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getOblivion} of the gas station.
	 */
	public final static MethodSignature GET_OBLIVION = new NonVoidMethodSignature(ClassType.GAS_STATION, "getOblivion", BasicTypes.LONG);

	/**
	 * The method {@code getStake} of the validators object.
	 */
	public final static MethodSignature GET_STAKE = new NonVoidMethodSignature(ClassType.VALIDATORS, "getStake", ClassType.BIG_INTEGER, ClassType.VALIDATOR);

	/**
	 * The method {@code getInitialInflation} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_INFLATION = new NonVoidMethodSignature(ClassType.VALIDATORS, "getInitialInflation", BasicTypes.LONG);

	/**
	 * The method {@code getCurrentInflation} of the validators object.
	 */
	public final static MethodSignature GET_CURRENT_INFLATION = new NonVoidMethodSignature(ClassType.VALIDATORS, "getCurrentInflation", BasicTypes.LONG);

	/**
	 * The method {@code ignoresGasPrice} of the gas station.
	 */
	public final static MethodSignature IGNORES_GAS_PRICE = new NonVoidMethodSignature(ClassType.GAS_STATION, "ignoresGasPrice", BasicTypes.BOOLEAN);

	/**
	 * The method {@code getValidators} of the manifest.
	 */
	public final static MethodSignature GET_VALIDATORS = new NonVoidMethodSignature(ClassType.MANIFEST, "getValidators", ClassType.VALIDATORS);

	/**
	 * The method {@code getInitialValidators} of the manifest.
	 */
	public final static MethodSignature GET_INITIAL_VALIDATORS = new NonVoidMethodSignature(ClassType.MANIFEST, "getInitialValidators", ClassType.SHARED_ENTITY_VIEW);

	/**
	 * The method {@code getVerificationVersion} of the versions object.
	 */
	public final static MethodSignature GET_VERIFICATION_VERSION = new NonVoidMethodSignature(ClassType.VERSIONS, "getVerificationVersion", BasicTypes.INT);

	/**
	 * The method {@code getPolls} of the validators object.
	 */
	public final static MethodSignature GET_POLLS = new NonVoidMethodSignature(ClassType.VALIDATORS, "getPolls", ClassType.STORAGE_SET_VIEW);

	/**
	 * The method {@code getInitialSupply} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_SUPPLY = new NonVoidMethodSignature(ClassType.VALIDATORS, "getInitialSupply", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getInitialRedSupply} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_RED_SUPPLY = new NonVoidMethodSignature(ClassType.VALIDATORS, "getInitialRedSupply", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getFinalSupply} of the validators object.
	 */
	public final static MethodSignature GET_FINAL_SUPPLY = new NonVoidMethodSignature(ClassType.VALIDATORS, "getFinalSupply", ClassType.BIG_INTEGER);

	/**
	 * The method {@code add} of the account ledger.
	 */
	public final static MethodSignature ADD_INTO_ACCOUNTS_LEDGER = new NonVoidMethodSignature(ClassType.ACCOUNTS_LEDGER, "add", ClassType.EOA, ClassType.BIG_INTEGER, ClassType.STRING);

	/**
	 * The method {@code id} of a validator.
	 */
	public final static MethodSignature ID = new NonVoidMethodSignature(ClassType.VALIDATOR, "id", ClassType.STRING);

	/**
	 * The method {@code mint} of an externally owned account.
	 */
	public final static MethodSignature EOA_MINT = new VoidMethodSignature(ClassType.EOA, "mint", ClassType.BIG_INTEGER);

	/**
	 * The method {@code burn} of an externally owned account.
	 */
	public final static MethodSignature EOA_BURN = new VoidMethodSignature(ClassType.EOA, "burn", ClassType.BIG_INTEGER);

	/**
	 * The method {@code receive} of a payable contract, with a big integer argument.
	 */
	public final static MethodSignature RECEIVE_BIG_INTEGER = new VoidMethodSignature(ClassType.PAYABLE_CONTRACT, "receive", ClassType.BIG_INTEGER);

	/**
	 * The method {@code receiveRed} of a payable contract, with a big integer argument.
	 */
	public final static MethodSignature RECEIVE_RED_BIG_INTEGER = new VoidMethodSignature(ClassType.PAYABLE_CONTRACT, "receiveRed", ClassType.BIG_INTEGER);

	/**
	 * The method {@code receive} of a payable contract, with an int argument.
	 */
	public final static MethodSignature RECEIVE_INT = new VoidMethodSignature(ClassType.PAYABLE_CONTRACT, "receive", BasicTypes.INT);

	/**
	 * The method {@code receive} of a payable contract, with a long argument.
	 */
	public final static MethodSignature RECEIVE_LONG = new VoidMethodSignature(ClassType.PAYABLE_CONTRACT, "receive", BasicTypes.LONG);

	/**
	 * The method {@code reward} of the validators contract.
	 */
	public final static MethodSignature VALIDATORS_REWARD = new VoidMethodSignature
		(ClassType.VALIDATORS, "reward", ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.STRING, ClassType.STRING, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER);

	/**
	 * The method {@code newPoll} of the generic validators contract.
	 */
	public final static MethodSignature NEW_POLL = new NonVoidMethodSignature(ClassType.GENERIC_VALIDATORS, "newPoll", ClassType.POLL);
	
	/**
	 * The method {@code newPollWithTimeParams} of the generic validators contract with time parameters.
	 */
	public final static MethodSignature NEW_POLL_WITH_TIME_PARAMS = new NonVoidMethodSignature(ClassType.GENERIC_VALIDATORS, "newPollWithTimeParams", ClassType.POLL, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER);
	
	/**
	 * The method {@code isVoteOver} of the Poll contract.
	 */
	public final static MethodSignature IS_VOTE_OVER = new NonVoidMethodSignature(ClassType.POLL, "isVoteOver", BasicTypes.BOOLEAN);
	
	/**
	 * The method {@code closePoll} of the Poll contract.
	 */
	public final static MethodSignature CLOSE_POLL = new VoidMethodSignature(ClassType.POLL, "closePoll");
	
	/**
	 * The method {@code vote} of the Poll contract.
	 */
	public final static MethodSignature VOTE = new VoidMethodSignature(ClassType.POLL, "vote");
	
	/**
	 * The method {@code vote} of the Poll contract with the share parameter.
	 */
	public final static MethodSignature VOTE_WITH_SHARE = new VoidMethodSignature(ClassType.POLL, "vote", ClassType.BIG_INTEGER);
	
	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	protected CodeSignature(ClassType definingClass, StorageType... formals) {
		if (definingClass == null)
			throw new IllegalArgumentException("definingClass cannot be null");

		if (formals == null)
			throw new IllegalArgumentException("formals cannot be null");

		for (StorageType formal: formals)
			if (formal == null)
				throw new IllegalArgumentException("formals cannot hold null");

		this.definingClass = definingClass;
		this.formals = formals;
	}

	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the name of the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	public CodeSignature(String definingClass, StorageType... formals) {
		this(new ClassType(definingClass), formals);
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
		return other instanceof CodeSignature && ((CodeSignature) other).definingClass.equals(definingClass)
			&& Arrays.equals(((CodeSignature) other).formals, formals);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ Arrays.hashCode(formals);
	}

	/**
	 * Yields the size of this code signature, in terms of gas units consumed in store.
	 * 
	 * @param gasCostModel the model of the costs
	 * @return the size
	 */
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot())
			.add(definingClass.size(gasCostModel))
			.add(formals().map(type -> type.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		definingClass.into(context);
		context.writeCompactInt(formals.length);
		for (StorageType formal: formals)
			formal.into(context);
	}

	/**
	 * Factory method that unmarshals a code signature from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the code signature
	 * @throws IOException if the code signature could not be unmarshalled
	 */
	public static CodeSignature from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();
		if (selector == ConstructorSignature.SELECTOR_EOA)
			return ConstructorSignature.EOA_CONSTRUCTOR;
		else if (selector == VoidMethodSignature.SELECTOR_REWARD)
			return VoidMethodSignature.VALIDATORS_REWARD;

		ClassType definingClass = (ClassType) StorageType.from(context);
		int formalsCount = context.readCompactInt();
		StorageType[] formals = new StorageType[formalsCount];
		for (int pos = 0; pos < formalsCount; pos++)
			formals[pos] = StorageType.from(context);

		switch (selector) {
		case ConstructorSignature.SELECTOR: return new ConstructorSignature(definingClass, formals);
		case VoidMethodSignature.SELECTOR: return new VoidMethodSignature(definingClass, context.readUTF(), formals);
		case NonVoidMethodSignature.SELECTOR: return new NonVoidMethodSignature(definingClass, context.readUTF(), StorageType.from(context), formals);
		default: throw new IOException("unexpected code signature selector: " + selector);
		}
	}
}