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

import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.BOOLEAN;
import static io.hotmoka.node.StorageTypes.GAMETE;
import static io.hotmoka.node.StorageTypes.GAS_STATION;
import static io.hotmoka.node.StorageTypes.INT;
import static io.hotmoka.node.StorageTypes.LONG;
import static io.hotmoka.node.StorageTypes.MANIFEST;
import static io.hotmoka.node.StorageTypes.OBJECT;
import static io.hotmoka.node.StorageTypes.STRING;
import static io.hotmoka.node.StorageTypes.VALIDATORS;
import static io.hotmoka.node.StorageTypes.DISK_VALIDATORS;
import static io.hotmoka.node.StorageTypes.MOKAMINT_VALIDATORS;
import static io.hotmoka.node.StorageTypes.TENDERMINT_VALIDATORS;

import java.io.IOException;

import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.json.MethodSignatureJson;
import io.hotmoka.node.internal.signatures.AbstractMethodSignature;
import io.hotmoka.node.internal.signatures.NonVoidMethodSignatureImpl;
import io.hotmoka.node.internal.signatures.VoidMethodSignatureImpl;
import io.hotmoka.websockets.beans.MappedDecoder;
import io.hotmoka.websockets.beans.MappedEncoder;

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
	public static class Encoder extends MappedEncoder<MethodSignature, Json> {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {
			super(Json::new);
		}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends MappedDecoder<MethodSignature, Json> {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {
			super(Json.class);
		}
	}

    /**
     * JSON representation.
     */
    public static class Json extends MethodSignatureJson {

    	/**
    	 * Creates the JSON representation for the given method signature.
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
	public final static NonVoidMethodSignature BALANCE = ofNonVoid(StorageTypes.CONTRACT, "balance", BIG_INTEGER);

	/**
	 * The method {@code publicKey} of an account.
	 */
	public final static NonVoidMethodSignature PUBLIC_KEY = ofNonVoid(StorageTypes.ACCOUNT, "publicKey", STRING);

	/**
	 * The method {@code rotatePublicKey} of an account.
	 */
	public final static VoidMethodSignature ROTATE_PUBLIC_KEY = ofVoid(StorageTypes.ACCOUNT, "rotatePublicKey", STRING);

	/**
	 * The method {@code nonce} of an account.
	 */
	public final static NonVoidMethodSignature NONCE = ofNonVoid(StorageTypes.ACCOUNT, "nonce", BIG_INTEGER);

	/**
	 * The method {@code getGenesisTime} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GENESIS_TIME = ofNonVoid(MANIFEST, "getGenesisTime", STRING);

	/**
	 * The method {@code getChainId} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_CHAIN_ID = ofNonVoid(MANIFEST, "getChainId", STRING);

	/**
	 * The method {@code getMaxDependencies} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_DEPENDENCIES = ofNonVoid(MANIFEST, "getMaxDependencies", INT);

	/**
	 * The method {@code getMaxCumulativeSizeOfDependencies} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES = ofNonVoid(MANIFEST, "getMaxCumulativeSizeOfDependencies", LONG);

	/**
	 * The method {@code getMaxRequestSize} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_MAX_REQUEST_SIZE = ofNonVoid(MANIFEST, "getMaxRequestSize", LONG);

	/**
	 * The method {@code getTicketForNewPoll} of the validators.
	 */
	public final static NonVoidMethodSignature GET_TICKET_FOR_NEW_POLL = ofNonVoid(VALIDATORS, "getTicketForNewPoll", BIG_INTEGER);

	/**
	 * The method {@code getHeight} of the validators.
	 */
	public final static NonVoidMethodSignature GET_HEIGHT = ofNonVoid(VALIDATORS, "getHeight", BIG_INTEGER);

	/**
	 * The method {@code getCurrentSupply} of the validators.
	 */
	public final static NonVoidMethodSignature GET_CURRENT_SUPPLY = ofNonVoid(VALIDATORS, "getCurrentSupply", BIG_INTEGER);

	/**
	 * The method {@code getNumberOfTransactions} of the validators.
	 */
	public final static NonVoidMethodSignature GET_NUMBER_OF_TRANSACTIONS = ofNonVoid(VALIDATORS, "getNumberOfTransactions", BIG_INTEGER);

	/**
	 * The method {@code getMaxFaucet} of the gamete.
	 */
	public final static NonVoidMethodSignature GET_MAX_FAUCET = ofNonVoid(GAMETE, "getMaxFaucet", BIG_INTEGER);

	/**
	 * The method {@code allowsUnsignedFaucet} of the manifest.
	 */
	public final static NonVoidMethodSignature ALLOWS_UNSIGNED_FAUCET = ofNonVoid(MANIFEST, "allowsUnsignedFaucet", BOOLEAN);

	/**
	 * The method {@code skipsVerification} of the manifest.
	 */
	public final static NonVoidMethodSignature SKIPS_VERIFICATION = ofNonVoid(MANIFEST, "skipsVerification", BOOLEAN);

	/**
	 * The method {@code getSignature} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_SIGNATURE = ofNonVoid(MANIFEST, "getSignature", STRING);

	/**
	 * The method {@code getGamete} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GAMETE = ofNonVoid(MANIFEST, "getGamete", StorageTypes.GAMETE);

	/**
	 * The method {@code getGasStation} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_GAS_STATION = ofNonVoid(MANIFEST, "getGasStation", GAS_STATION);

	/**
	 * The method {@code getVersions} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_VERSIONS = ofNonVoid(MANIFEST, "getVersions", StorageTypes.VERSIONS);

	/**
	 * The method {@code getAccountsLedger} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_ACCOUNTS_LEDGER = ofNonVoid(MANIFEST, "getAccountsLedger", StorageTypes.ACCOUNTS_LEDGER);

	/**
	 * The method {@code get} of the account ledger.
	 */
	public final static NonVoidMethodSignature GET_FROM_ACCOUNTS_LEDGER = ofNonVoid(StorageTypes.ACCOUNTS_LEDGER, "get", StorageTypes.EOA, STRING);

	/**
	 * The method {@code getGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_GAS_PRICE = ofNonVoid(GAS_STATION, "getGasPrice", BIG_INTEGER);

	/**
	 * The method {@code getMaxGasPerTransaction} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_MAX_GAS_PER_TRANSACTION = ofNonVoid(GAS_STATION, "getMaxGasPerTransaction", BIG_INTEGER);

	/**
	 * The method {@code getInitialGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_GAS_PRICE = ofNonVoid(GAS_STATION, "getInitialGasPrice", BIG_INTEGER);

	/**
	 * The method {@code getTargetGasAtReward} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_TARGET_GAS_AT_REWARD = ofNonVoid(GAS_STATION, "getTargetGasAtReward", BIG_INTEGER);

	/**
	 * The method {@code getOblivion} of the gas station.
	 */
	public final static NonVoidMethodSignature GET_OBLIVION = ofNonVoid(GAS_STATION, "getOblivion", LONG);

	/**
	 * The method {@code getStake} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_STAKE = ofNonVoid(VALIDATORS, "getStake", BIG_INTEGER, StorageTypes.VALIDATOR);

	/**
	 * The method {@code getShares} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_SHARES = ofNonVoid(VALIDATORS, "getShares", StorageTypes.STORAGE_MAP_VIEW);

	/**
	 * The method {@code ignoresGasPrice} of the gas station.
	 */
	public final static NonVoidMethodSignature IGNORES_GAS_PRICE = ofNonVoid(GAS_STATION, "ignoresGasPrice", BOOLEAN);

	/**
	 * The method {@code getValidators} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_VALIDATORS = ofNonVoid(MANIFEST, "getValidators", VALIDATORS);

	/**
	 * The method {@code getInitialValidators} of the manifest.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_VALIDATORS = ofNonVoid(MANIFEST, "getInitialValidators", StorageTypes.SHARED_ENTITY_VIEW);

	/**
	 * The method {@code getVerificationVersion} of the versions object.
	 */
	public final static NonVoidMethodSignature GET_VERIFICATION_VERSION = ofNonVoid(StorageTypes.VERSIONS, "getVerificationVersion", LONG);

	/**
	 * The method {@code getPolls} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_POLLS = ofNonVoid(VALIDATORS, "getPolls", StorageTypes.STORAGE_SET_VIEW);

	/**
	 * The method {@code getInitialSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_INITIAL_SUPPLY = ofNonVoid(VALIDATORS, "getInitialSupply", BIG_INTEGER);

	/**
	 * The method {@code getFinalSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_FINAL_SUPPLY = ofNonVoid(VALIDATORS, "getFinalSupply", BIG_INTEGER);

	/**
	 * The method {@code getHeightAtFinalSupply} of the validators object.
	 */
	public final static NonVoidMethodSignature GET_HEIGHT_AT_FINAL_SUPPLY = ofNonVoid(VALIDATORS, "getHeightAtFinalSupply", BIG_INTEGER);

	/**
	 * The method {@code add} of the account ledger.
	 */
	public final static NonVoidMethodSignature ADD_INTO_ACCOUNTS_LEDGER = ofNonVoid(StorageTypes.ACCOUNTS_LEDGER, "add", StorageTypes.EOA, BIG_INTEGER, STRING);

	/**
	 * The method {@code id} of a validator.
	 */
	public final static NonVoidMethodSignature ID = ofNonVoid(StorageTypes.VALIDATOR, "id", STRING);

	/**
	 * The method {@code receive} of a payable contract, with a big integer argument.
	 */
	public final static VoidMethodSignature RECEIVE_BIGINTEGER = ofVoid(StorageTypes.PAYABLE_CONTRACT, "receive", BIG_INTEGER);

	/**
	 * The method {@code receive} of a payable contract, with an {@code int} argument.
	 */
	public final static VoidMethodSignature RECEIVE_INT = ofVoid(StorageTypes.PAYABLE_CONTRACT, "receive", INT);

	/**
	 * The method {@code receive} of a payable contract, with a {@code long} argument.
	 */
	public final static VoidMethodSignature RECEIVE_LONG = ofVoid(StorageTypes.PAYABLE_CONTRACT, "receive", LONG);

	/**
	 * The method {@code reward} of the Tendermint validators contract.
	 */
	public final static VoidMethodSignature TENDERMINT_VALIDATORS_REWARD = ofVoid(TENDERMINT_VALIDATORS, "reward", BIG_INTEGER, BIG_INTEGER, STRING, STRING, BIG_INTEGER, BIG_INTEGER);

	/**
	 * The method {@code reward} of the disk node validators contract.
	 */
	public final static VoidMethodSignature DISK_VALIDATORS_REWARD = ofVoid(DISK_VALIDATORS, "reward", BIG_INTEGER, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER);

	/**
	 * The method {@code rewardMokamint} of the validators contract.
	 */
	public final static NonVoidMethodSignature MOKAMINT_VALIDATORS_REWARD = ofNonVoid(MOKAMINT_VALIDATORS, "rewardMokamint", BOOLEAN, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER, STRING, STRING, BIG_INTEGER, BIG_INTEGER);

	/**
	 * The method {@code rewardMokamintMiner} of the validators contract.
	 */
	public final static VoidMethodSignature MOKAMINT_VALIDATORS_REWARD_MINER = ofVoid(MOKAMINT_VALIDATORS, "rewardMokamintMiner", BIG_INTEGER, STRING);

	/**
	 * The method {@code getBuyerSurcharge} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_BUYER_SURCHARGE = ofNonVoid(VALIDATORS, "getBuyerSurcharge", INT);

	/**
	 * The method {@code getSlashingForMisbehaving} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_SLASHING_FOR_MISBEHAVING = ofNonVoid(VALIDATORS, "getSlashingForMisbehaving", INT);

	/**
	 * The method {@code getSlashingForNotBehaving} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING = ofNonVoid(VALIDATORS, "getSlashingForNotBehaving", INT);

	/**
	 * The method {@code getPercentStaked} of the validators contract.
	 */
	public final static NonVoidMethodSignature VALIDATORS_GET_PERCENT_STAKED = ofNonVoid(VALIDATORS, "getPercentStaked", INT);

	/**
	 * The method {@code newPoll} of the generic validators contract.
	 */
	public final static NonVoidMethodSignature NEW_POLL = ofNonVoid(StorageTypes.GENERIC_VALIDATORS, "newPoll", StorageTypes.POLL);
	
	/**
	 * The method {@code newPollWithTimeParams} of the generic validators contract with time parameters.
	 */
	public final static NonVoidMethodSignature NEW_POLL_WITH_TIME_PARAMS = ofNonVoid(StorageTypes.GENERIC_VALIDATORS, "newPollWithTimeParams", StorageTypes.POLL, BIG_INTEGER, BIG_INTEGER);
	
	/**
	 * The method {@code isVoteOver} of a {@code Poll} contract.
	 */
	public final static NonVoidMethodSignature IS_VOTE_OVER = ofNonVoid(StorageTypes.POLL, "isVoteOver", BOOLEAN);
	
	/**
	 * The method {@code closePoll} of a {@code Poll} contract.
	 */
	public final static VoidMethodSignature CLOSE_POLL = ofVoid(StorageTypes.POLL, "closePoll");
	
	/**
	 * The method {@code vote} of a {@code Poll} contract.
	 */
	public final static VoidMethodSignature VOTE = ofVoid(StorageTypes.POLL, "vote");
	
	/**
	 * The method {@code vote} of a {@code Poll} contract with the share parameter.
	 */
	public final static VoidMethodSignature VOTE_WITH_SHARE = ofVoid(StorageTypes.POLL, "vote", BIG_INTEGER);

	/**
	 * The method {@code size} of a {@code StorageMapView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_SIZE = ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "size", INT);

	/**
	 * The method {@code select} of a {@code StorageMapView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_SELECT = ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "select", OBJECT, INT);

	/**
	 * The method {@code get} of a {@code StorageMapView} contract.
	 */	
	public final static NonVoidMethodSignature STORAGE_MAP_VIEW_GET = ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "get", OBJECT, OBJECT);

	/**
	 * The method {@code select} of a {@code StorageSetView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_SET_VIEW_SELECT = ofNonVoid(StorageTypes.STORAGE_SET_VIEW, "select", OBJECT, INT);

	/**
	 * The method {@code szie} of a {@code StorageSetView} contract.
	 */
	public final static NonVoidMethodSignature STORAGE_SET_VIEW_SIZE = ofNonVoid(StorageTypes.STORAGE_SET_VIEW, "size", INT);
}