/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node;

import java.io.IOException;
import java.util.stream.Stream;

import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.gson.MethodSignatureDecoder;
import io.hotmoka.node.internal.gson.MethodSignatureEncoder;
import io.hotmoka.node.internal.gson.MethodSignatureJson;
import io.hotmoka.node.internal.signatures.AbstractMethodSignature;
import io.hotmoka.node.internal.signatures.NonVoidMethodSignatureImpl;
import io.hotmoka.node.internal.signatures.VoidMethodSignatureImpl;

/**
 * Providers of method signatures.
 */
public abstract class MethodSignatures {

	private MethodSignatures() {}

	/**
	 * Yields the signature of a method, that returns a value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 * @return the signature of the method
	 */
	public static NonVoidMethodSignature ofNonVoid(ClassType definingClass, String methodName, StorageType returnType, StorageType... formals) {
		return new NonVoidMethodSignatureImpl(definingClass, methodName, returnType, formals);
	}

	/**
	 * Yields the signature of a method, that returns a value.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 * @return the signature of the method
	 */
	public static NonVoidMethodSignature ofNonVoid(String definingClass, String methodName, StorageType returnType, StorageType... formals) {
		return new NonVoidMethodSignatureImpl(StorageTypes.classNamed(definingClass), methodName, returnType, formals);
	}

	/**
	 * Yields the signature of a method, that returns a value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 * @return the signature of the method
	 */
	public static NonVoidMethodSignature ofNonVoid(ClassType definingClass, String methodName, StorageType returnType, Stream<StorageType> formals) {
		return new NonVoidMethodSignatureImpl(definingClass, methodName, returnType, formals.toArray(StorageType[]::new));
	}

	/**
	 * Yields the signature of a method, that returns a value.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 * @return the signature of the method
	 */
	public static NonVoidMethodSignature ofNonVoid(String definingClass, String methodName, StorageType returnType, Stream<StorageType> formals) {
		return new NonVoidMethodSignatureImpl(StorageTypes.classNamed(definingClass), methodName, returnType, formals.toArray(StorageType[]::new));
	}

	/**
	 * Yields the signature of a method, that returns no value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 * @return the signature of the method
	 */
	public static VoidMethodSignature ofVoid(ClassType definingClass, String methodName, StorageType... formals) {
		return new VoidMethodSignatureImpl(definingClass, methodName, formals);
	}

	/**
	 * Yields the signature of a method, that returns no value.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 * @return the signature of the method
	 */
	public static VoidMethodSignature ofVoid(String definingClass, String methodName, StorageType... formals) {
		return new VoidMethodSignatureImpl(StorageTypes.classNamed(definingClass), methodName, formals);
	}

	/**
	 * Yields the signature of a method, that returns no value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 * @return the signature of the method
	 */
	public static VoidMethodSignature ofVoid(ClassType definingClass, String methodName, Stream<StorageType> formals) {
		return new VoidMethodSignatureImpl(definingClass, methodName, formals.toArray(StorageType[]::new));
	}

	/**
	 * Yields the signature of a method, that returns no value.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 * @return the signature of the method
	 */
	public static VoidMethodSignature ofVoid(String definingClass, String methodName, Stream<StorageType> formals) {
		return new VoidMethodSignatureImpl(StorageTypes.classNamed(definingClass), methodName, formals.toArray(StorageType[]::new));
	}

	/**
	 * Unmarshals a constructor signature from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the constructor signature
	 * @throws IOException if the constructor signature cannot be unmarshalled
	 */
	public static MethodSignature from(UnmarshallingContext context) throws IOException {
		return AbstractMethodSignature.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends MethodSignatureEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends MethodSignatureDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends MethodSignatureJson {

    	/**
    	 * Creates the Json representation for the given method signature.
    	 * 
    	 * @param method the method signature
    	 */
    	public Json(MethodSignature method) {
    		super(method);
    	}
    }

    /**
	 * The method {@code balance} of a contract.
	 */
	public final static NonVoidMethodSignature BALANCE = AbstractMethodSignature.BALANCE;

	/**
	 * The method {@code balanceRed} of a contract.
	 */
	public final static NonVoidMethodSignature BALANCE_RED = AbstractMethodSignature.BALANCE_RED;

	/**
	 * The method {@code publicKey} of an account.
	 */
	public final static NonVoidMethodSignature PUBLIC_KEY = AbstractMethodSignature.PUBLIC_KEY;

	/**
	 * The method {@code nonce} of an account.
	 */
	public final static NonVoidMethodSignature NONCE = AbstractMethodSignature.NONCE;

	/**
	 * The method {@code getGenesisTime} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GENESIS_TIME = AbstractMethodSignature.GET_GENESIS_TIME;

	/**
	 * The method {@code getChainId} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_CHAIN_ID = AbstractMethodSignature.GET_CHAIN_ID;

	/**
	 * The method {@code getMaxErrorLength} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_ERROR_LENGTH = AbstractMethodSignature.GET_MAX_ERROR_LENGTH;

	/**
	 * The method {@code getMaxDependencies} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_DEPENDENCIES = AbstractMethodSignature.GET_MAX_DEPENDENCIES;

	/**
	 * The method {@code getMaxCumulativeSizeOfDependencies} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES = AbstractMethodSignature.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES;

	/**
	 * The method {@code getTicketForNewPoll} of the validators.
	 */
	public final static NonVoidMethodSignature GET_TICKET_FOR_NEW_POLL = AbstractMethodSignature.GET_TICKET_FOR_NEW_POLL;

	/**
	 * The method {@code getHeight} of the validators.
	 */
	public final static NonVoidMethodSignature GET_HEIGHT = AbstractMethodSignature.GET_HEIGHT;

	/**
	 * The method {@code getCurrentSupply} of the validators.
	 */
	public final static NonVoidMethodSignature GET_CURRENT_SUPPLY = AbstractMethodSignature.GET_CURRENT_SUPPLY;

	/**
	 * The method {@code getNumberOfTransactions} of the validators.
	 */
	public final static NonVoidMethodSignature GET_NUMBER_OF_TRANSACTIONS = AbstractMethodSignature.GET_NUMBER_OF_TRANSACTIONS;

	/**
	 * The method {@code getMaxFaucet} of the gamete.
	 */
	public final static NonVoidMethodSignature GET_MAX_FAUCET = AbstractMethodSignature.GET_MAX_FAUCET;

	/**
	 * The method {@code getMaxRedFaucet} of the gamete.
	 */
	public final static NonVoidMethodSignature GET_MAX_RED_FAUCET = AbstractMethodSignature.GET_MAX_RED_FAUCET;

	/**
	 * The method {@code allowsUnsignedFaucet} of the manifest.
	 */
	public final static NonVoidMethodSignature ALLOWS_UNSIGNED_FAUCET = AbstractMethodSignature.ALLOWS_UNSIGNED_FAUCET;

	/**
	 * The method {@code skipsVerification} of the manifest.
	 */
	public final static NonVoidMethodSignature SKIPS_VERIFICATION = AbstractMethodSignature.SKIPS_VERIFICATION;

	/**
	 * The method {@code getSignature} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_SIGNATURE = AbstractMethodSignature.GET_SIGNATURE;

	/**
	 * The method {@code getGamete} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GAMETE = AbstractMethodSignature.GET_GAMETE;

	/**
	 * The method {@code getGasStation} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GAS_STATION = AbstractMethodSignature.GET_GAS_STATION;

	/**
	 * The method {@code getVersions} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_VERSIONS = AbstractMethodSignature.GET_VERSIONS;

	/**
	 * The method {@code getAccountsLedger} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_ACCOUNTS_LEDGER = AbstractMethodSignature.GET_ACCOUNTS_LEDGER;

	/**
	 * The method {@code get} of the account ledger.
	 */
	public final static NonVoidMethodSignature GET_FROM_ACCOUNTS_LEDGER = AbstractMethodSignature.GET_FROM_ACCOUNTS_LEDGER;

	/**
	 * The method {@code getGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_GAS_PRICE = AbstractMethodSignature.GET_GAS_PRICE;

	/**
	 * The method {@code getMaxGasPerTransaction} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_MAX_GAS_PER_TRANSACTION = AbstractMethodSignature.GET_MAX_GAS_PER_TRANSACTION;

	/**
	 * The method {@code getInitialGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_GAS_PRICE = AbstractMethodSignature.GET_INITIAL_GAS_PRICE;

	/**
	 * The method {@code getTargetGasAtReward} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_TARGET_GAS_AT_REWARD = AbstractMethodSignature.GET_TARGET_GAS_AT_REWARD;

	/**
	 * The method {@code getOblivion} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_OBLIVION = AbstractMethodSignature.GET_OBLIVION;

	/**
	 * The method {@code getStake} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_STAKE = AbstractMethodSignature.GET_STAKE;

	/**
	 * The method {@code getInitialInflation} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_INFLATION = AbstractMethodSignature.GET_INITIAL_INFLATION;

	/**
	 * The method {@code getCurrentInflation} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_CURRENT_INFLATION = AbstractMethodSignature.GET_CURRENT_INFLATION;

	/**
	 * The method {@code getShares} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_SHARES = AbstractMethodSignature.GET_SHARES;

	/**
	 * The method {@code ignoresGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature IGNORES_GAS_PRICE = AbstractMethodSignature.IGNORES_GAS_PRICE;

	/**
	 * The method {@code getValidators} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_VALIDATORS = AbstractMethodSignature.GET_VALIDATORS;

	/**
	 * The method {@code getInitialValidators} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_VALIDATORS = AbstractMethodSignature.GET_INITIAL_VALIDATORS;

	/**
	 * The method {@code getVerificationVersion} of the versions object.
	 */
	public final static NonVoidMethodSignature GET_VERIFICATION_VERSION = AbstractMethodSignature.GET_VERIFICATION_VERSION;

	/**
	 * The method {@code getPolls} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_POLLS = AbstractMethodSignature.GET_POLLS;

	/**
	 * The method {@code getInitialSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_SUPPLY = AbstractMethodSignature.GET_INITIAL_SUPPLY;

	/**
	 * The method {@code getInitialRedSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_RED_SUPPLY = AbstractMethodSignature.GET_INITIAL_RED_SUPPLY;

	/**
	 * The method {@code getFinalSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_FINAL_SUPPLY = AbstractMethodSignature.GET_FINAL_SUPPLY;

	/**
	 * The method {@code add} of the account ledger.
	 */
	public final static NonVoidMethodSignature ADD_INTO_ACCOUNTS_LEDGER = AbstractMethodSignature.ADD_INTO_ACCOUNTS_LEDGER;

	/**
	 * The method {@code id} of a validator.
	 */
	public final static NonVoidMethodSignature ID = AbstractMethodSignature.ID;

	/**
	 * The method {@code receive} of a payable contract, with a big integer argument.
	 */
	public final static VoidMethodSignature RECEIVE_BIG_INTEGER = AbstractMethodSignature.RECEIVE_BIG_INTEGER;

	/**
	 * The method {@code receiveRed} of a payable contract, with a big integer argument.
	 */
	public final static VoidMethodSignature RECEIVE_RED_BIG_INTEGER = AbstractMethodSignature.RECEIVE_RED_BIG_INTEGER;

	/**
	 * The method {@code receive} of a payable contract, with an {@code int} argument.
	 */
	public final static VoidMethodSignature RECEIVE_INT = AbstractMethodSignature.RECEIVE_INT;

	/**
	 * The method {@code receive} of a payable contract, with a {@code long} argument.
	 */
	public final static VoidMethodSignature RECEIVE_LONG = AbstractMethodSignature.RECEIVE_LONG;

	/**
	 * The method {@code reward} of the validators contract.
	 */
	public final static VoidMethodSignature VALIDATORS_REWARD = AbstractMethodSignature.VALIDATORS_REWARD;

	/**
	 * The method {@code rewardMokamintNode} of the validators contract.
	 */
	public final static VoidMethodSignature VALIDATORS_REWARD_MOKAMINT_NODE = AbstractMethodSignature.VALIDATORS_REWARD_MOKAMINT_NODE;

	/**
	 * The method {@code rewardMokamintMiner} of the validators contract.
	 */
	public final static VoidMethodSignature VALIDATORS_REWARD_MOKAMINT_MINER = AbstractMethodSignature.VALIDATORS_REWARD_MOKAMINT_MINER;

	/**
	 * The method {@code getBuyerSurcharge} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_BUYER_SURCHARGE = AbstractMethodSignature.VALIDATORS_GET_BUYER_SURCHARGE;

	/**
	 * The method {@code getSlashingForMisbehaving} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_SLASHING_FOR_MISBEHAVING = AbstractMethodSignature.VALIDATORS_GET_SLASHING_FOR_MISBEHAVING;

	/**
	 * The method {@code getSlashingForNotBehaving} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING = AbstractMethodSignature.VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING;

	/**
	 * The method {@code getPercentStaked} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_PERCENT_STAKED = AbstractMethodSignature.VALIDATORS_GET_PERCENT_STAKED;

	/**
	 * The method {@code newPoll} of the generic validators contract.
	 */
	public final static NonVoidMethodSignature NEW_POLL = AbstractMethodSignature.NEW_POLL;
	
	/**
	 * The method {@code newPollWithTimeParams} of the generic validators contract with time parameters.
	 */
	public final static NonVoidMethodSignature NEW_POLL_WITH_TIME_PARAMS = AbstractMethodSignature.NEW_POLL_WITH_TIME_PARAMS;
	
	/**
	 * The method {@code isVoteOver} of a {@code Poll} contract.
	 */
	public final static NonVoidMethodSignature IS_VOTE_OVER = AbstractMethodSignature.IS_VOTE_OVER;
	
	/**
	 * The method {@code closePoll} of a {@code Poll} contract.
	 */
	public final static VoidMethodSignature CLOSE_POLL = AbstractMethodSignature.CLOSE_POLL;
	
	/**
	 * The method {@code vote} of a {@code Poll} contract.
	 */
	public final static VoidMethodSignature VOTE = AbstractMethodSignature.VOTE;
	
	/**
	 * The method {@code vote} of a {@code Poll} contract with the share parameter.
	 */
	public final static VoidMethodSignature VOTE_WITH_SHARE = AbstractMethodSignature.VOTE_WITH_SHARE;

	/**
	 * The method {@code size} of a {@code StorageMapView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_SIZE = AbstractMethodSignature.STORAGE_MAP_VIEW_SIZE;

	/**
	 * The method {@code select} of a {@code StorageMapView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_SELECT = AbstractMethodSignature.STORAGE_MAP_VIEW_SELECT;

	/**
	 * The method {@code get} of a {@code StorageMapView} contract.
	 */	
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_GET = AbstractMethodSignature.STORAGE_MAP_VIEW_GET;

	/**
	 * The method {@code select} of a {@code StorageSetView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_SET_VIEW_SELECT = AbstractMethodSignature.STORAGE_SET_VIEW_SELECT;

	/**
	 * The method {@code szie} of a {@code StorageSetView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_SET_VIEW_SIZE = AbstractMethodSignature.STORAGE_SET_VIEW_SIZE;
}