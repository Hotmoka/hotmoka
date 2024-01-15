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

package io.hotmoka.beans;

import io.hotmoka.beans.signatures.MethodSignature;

/**
 * Providers of method signatures.
 */
public abstract class MethodSignatures {

	private MethodSignatures() {}

	/**
	 * The method {@code balance} of a contract.
	 */
	public final static MethodSignature BALANCE = MethodSignature.BALANCE;

	/**
	 * The method {@code balanceRed} of a contract.
	 */
	public final static MethodSignature BALANCE_RED = MethodSignature.BALANCE_RED;

	/**
	 * The method {@code publicKey} of an account.
	 */
	public final static MethodSignature PUBLIC_KEY = MethodSignature.PUBLIC_KEY;

	/**
	 * The method {@code nonce} of an account.
	 */
	public final static MethodSignature NONCE = MethodSignature.NONCE;

	/**
	 * The method {@code getGenesisTime} of the manifest.
	 */
	public final static MethodSignature GET_GENESIS_TIME = MethodSignature.GET_GENESIS_TIME;

	/**
	 * The method {@code getChainId} of the manifest.
	 */
	public final static MethodSignature GET_CHAIN_ID = MethodSignature.GET_CHAIN_ID;

	/**
	 * The method {@code getMaxErrorLength} of the manifest.
	 */
	public final static MethodSignature GET_MAX_ERROR_LENGTH = MethodSignature.GET_MAX_ERROR_LENGTH;

	/**
	 * The method {@code getMaxDependencies} of the manifest.
	 */
	public final static MethodSignature GET_MAX_DEPENDENCIES = MethodSignature.GET_MAX_DEPENDENCIES;

	/**
	 * The method {@code getMaxCumulativeSizeOfDependencies} of the manifest.
	 */
	public final static MethodSignature GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES = MethodSignature.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES;

	/**
	 * The method {@code getTicketForNewPoll} of the validators.
	 */
	public final static MethodSignature GET_TICKET_FOR_NEW_POLL = MethodSignature.GET_TICKET_FOR_NEW_POLL;

	/**
	 * The method {@code getHeight} of the validators.
	 */
	public final static MethodSignature GET_HEIGHT = MethodSignature.GET_HEIGHT;

	/**
	 * The method {@code getCurrentSupply} of the validators.
	 */
	public final static MethodSignature GET_CURRENT_SUPPLY = MethodSignature.GET_CURRENT_SUPPLY;

	/**
	 * The method {@code getNumberOfTransactions} of the validators.
	 */
	public final static MethodSignature GET_NUMBER_OF_TRANSACTIONS = MethodSignature.GET_NUMBER_OF_TRANSACTIONS;

	/**
	 * The method {@code getMaxFaucet} of the gamete.
	 */
	public final static MethodSignature GET_MAX_FAUCET = MethodSignature.GET_MAX_FAUCET;

	/**
	 * The method {@code getMaxRedFaucet} of the gamete.
	 */
	public final static MethodSignature GET_MAX_RED_FAUCET = MethodSignature.GET_MAX_RED_FAUCET;

	/**
	 * The method {@code allowsSelfCharged} of the manifest.
	 */
	public final static MethodSignature ALLOWS_SELF_CHARGED = MethodSignature.ALLOWS_SELF_CHARGED; // TODO: remove

	/**
	 * The method {@code allowsUnsignedFaucet} of the manifest.
	 */
	public final static MethodSignature ALLOWS_UNSIGNED_FAUCET = MethodSignature.ALLOWS_UNSIGNED_FAUCET;

	/**
	 * The method {@code skipsVerification} of the manifest.
	 */
	public final static MethodSignature SKIPS_VERIFICATION = MethodSignature.SKIPS_VERIFICATION;

	/**
	 * The method {@code getSignature} of the manifest.
	 */
	public final static MethodSignature GET_SIGNATURE = MethodSignature.GET_SIGNATURE;

	/**
	 * The method {@code getGamete} of the manifest.
	 */
	public final static MethodSignature GET_GAMETE = MethodSignature.GET_GAMETE;

	/**
	 * The method {@code getGasStation} of the manifest.
	 */
	public final static MethodSignature GET_GAS_STATION = MethodSignature.GET_GAS_STATION;

	/**
	 * The method {@code getVersions} of the manifest.
	 */
	public final static MethodSignature GET_VERSIONS = MethodSignature.GET_VERSIONS;

	/**
	 * The method {@code getAccountsLedger} of the manifest.
	 */
	public final static MethodSignature GET_ACCOUNTS_LEDGER = MethodSignature.GET_ACCOUNTS_LEDGER;

	/**
	 * The method {@code get} of the account ledger.
	 */
	public final static MethodSignature GET_FROM_ACCOUNTS_LEDGER = MethodSignature.GET_FROM_ACCOUNTS_LEDGER; // TODO: check name

	/**
	 * The method {@code getGasPrice} of the gas station.
	 */
	public final static MethodSignature GET_GAS_PRICE = MethodSignature.GET_GAS_PRICE;

	/**
	 * The method {@code getMaxGasPerTransaction} of the gas station.
	 */
	public final static MethodSignature GET_MAX_GAS_PER_TRANSACTION = MethodSignature.GET_MAX_GAS_PER_TRANSACTION;

	/**
	 * The method {@code getInitialGasPrice} of the gas station.
	 */
	public final static MethodSignature GET_INITIAL_GAS_PRICE = MethodSignature.GET_INITIAL_GAS_PRICE;

	/**
	 * The method {@code getTargetGasAtReward} of the gas station.
	 */
	public final static MethodSignature GET_TARGET_GAS_AT_REWARD = MethodSignature.GET_TARGET_GAS_AT_REWARD;

	/**
	 * The method {@code getOblivion} of the gas station.
	 */
	public final static MethodSignature GET_OBLIVION = MethodSignature.GET_OBLIVION;

	/**
	 * The method {@code getStake} of the validators object.
	 */
	public final static MethodSignature GET_STAKE = MethodSignature.GET_STAKE;

	/**
	 * The method {@code getInitialInflation} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_INFLATION = MethodSignature.GET_INITIAL_INFLATION;

	/**
	 * The method {@code getCurrentInflation} of the validators object.
	 */
	public final static MethodSignature GET_CURRENT_INFLATION = MethodSignature.GET_CURRENT_INFLATION;

	/**
	 * The method {@code ignoresGasPrice} of the gas station.
	 */
	public final static MethodSignature IGNORES_GAS_PRICE = MethodSignature.IGNORES_GAS_PRICE;

	/**
	 * The method {@code getValidators} of the manifest.
	 */
	public final static MethodSignature GET_VALIDATORS = MethodSignature.GET_VALIDATORS;

	/**
	 * The method {@code getInitialValidators} of the manifest.
	 */
	public final static MethodSignature GET_INITIAL_VALIDATORS = MethodSignature.GET_INITIAL_VALIDATORS;

	/**
	 * The method {@code getVerificationVersion} of the versions object.
	 */
	public final static MethodSignature GET_VERIFICATION_VERSION = MethodSignature.GET_VERIFICATION_VERSION;

	/**
	 * The method {@code getPolls} of the validators object.
	 */
	public final static MethodSignature GET_POLLS = MethodSignature.GET_POLLS;

	/**
	 * The method {@code getInitialSupply} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_SUPPLY = MethodSignature.GET_INITIAL_SUPPLY;

	/**
	 * The method {@code getInitialRedSupply} of the validators object.
	 */
	public final static MethodSignature GET_INITIAL_RED_SUPPLY = MethodSignature.GET_INITIAL_RED_SUPPLY;

	/**
	 * The method {@code getFinalSupply} of the validators object.
	 */
	public final static MethodSignature GET_FINAL_SUPPLY = MethodSignature.GET_FINAL_SUPPLY;

	/**
	 * The method {@code add} of the account ledger.
	 */
	public final static MethodSignature ADD_INTO_ACCOUNTS_LEDGER = MethodSignature.ADD_INTO_ACCOUNTS_LEDGER;

	/**
	 * The method {@code id} of a validator.
	 */
	public final static MethodSignature ID = MethodSignature.ID;

	/**
	 * The method {@code receive} of a payable contract, with a big integer argument.
	 */
	public final static MethodSignature RECEIVE_BIG_INTEGER = MethodSignature.RECEIVE_BIG_INTEGER;

	/**
	 * The method {@code receiveRed} of a payable contract, with a big integer argument.
	 */
	public final static MethodSignature RECEIVE_RED_BIG_INTEGER = MethodSignature.RECEIVE_RED_BIG_INTEGER;

	/**
	 * The method {@code receive} of a payable contract, with an {@code int} argument.
	 */
	public final static MethodSignature RECEIVE_INT = MethodSignature.RECEIVE_INT;

	/**
	 * The method {@code receive} of a payable contract, with a {@code long} argument.
	 */
	public final static MethodSignature RECEIVE_LONG = MethodSignature.RECEIVE_LONG;

	/**
	 * The method {@code reward} of the validators contract.
	 */
	public final static MethodSignature VALIDATORS_REWARD = MethodSignature.VALIDATORS_REWARD;

	/**
	 * The method {@code newPoll} of the generic validators contract.
	 */
	public final static MethodSignature NEW_POLL = MethodSignature.NEW_POLL;
	
	/**
	 * The method {@code newPollWithTimeParams} of the generic validators contract with time parameters.
	 */
	public final static MethodSignature NEW_POLL_WITH_TIME_PARAMS = MethodSignature.NEW_POLL_WITH_TIME_PARAMS;
	
	/**
	 * The method {@code isVoteOver} of a {@code Poll} contract.
	 */
	public final static MethodSignature IS_VOTE_OVER = MethodSignature.IS_VOTE_OVER;
	
	/**
	 * The method {@code closePoll} of a {@code Poll} contract.
	 */
	public final static MethodSignature CLOSE_POLL = MethodSignature.CLOSE_POLL;
	
	/**
	 * The method {@code vote} of a {@code Poll} contract.
	 */
	public final static MethodSignature VOTE = MethodSignature.VOTE;
	
	/**
	 * The method {@code vote} of a {@code Poll} contract with the share parameter.
	 */
	public final static MethodSignature VOTE_WITH_SHARE = MethodSignature.VOTE_WITH_SHARE;
}