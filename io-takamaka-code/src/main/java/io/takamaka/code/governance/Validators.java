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

package io.takamaka.code.governance;

import java.math.BigInteger;

import io.takamaka.code.dao.Poll;
import io.takamaka.code.dao.PollWithTimeWindow;
import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.dao.SharedEntity.Offer;
import io.takamaka.code.dao.SimplePoll;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageSetView;

/**
 * The validators are the accounts that get rewarded at specific
 * intervals, for instance when a new block is committed in a blockchain.
 * Any update to the number or properties of the validators must generate
 * an event of type {@link ValidatorsUpdate}.
 * 
 * @param <V> the type of the validator contracts
 */
public interface Validators<V extends Validator> extends SharedEntity<V, Offer<V>> {

	/**
	 * Rewards validators that behaved correctly and punishes validators that
	 * misbehaved. Hotmoka nodes might call this method at regular
	 * intervals; for instance, after each committed block in a blockchain.
	 * Its goal is to reward the behaving validators and punish the
	 * misbehaving ones. Note that a validator might not be in
	 * {@code behaving} nor in {@code misbehaving} if, for instance, it
	 * failed to vote because it was down. The implementation of this
	 * method can decide what to do in that case.
	 * Normally, it is expected that the identifiers in {@code behaving}
	 * and {@code misbehaving} are those of validators in this validators set.
	 * 
	 * @param amount the amount to distribute to the validators
	 * @param minted the subset of {@code amount} that has been minted during the last reward;
	 *               this means that {@code amount} is the sum of gas costs incurred by the
	 *               payers of the transactions and an extra inflation that is exactly {@code minted}
	 * @param behaving space-separated identifiers of validators that behaved correctly
	 * @param misbehaving space-separated identifiers of validators that misbehaved
	 * @param gasConsumed the gas consumed for CPU, RAM usage or storage by the transactions
	 *                    executed since the previous reward
	 * @param numberOfTransactionsSinceLastReward the number of transactions executed since
	 *                                            the previous reward
	 */
	@FromContract @Payable void reward(BigInteger amount, BigInteger minted, String behaving, String misbehaving, BigInteger gasConsumed, BigInteger numberOfTransactionsSinceLastReward);

	/**
	 * Yields the earnings collected by the given validator and not yet sent to it.
	 * They are not given immediately to the validator but only when it sells all its shares.
	 * 
	 * @param validator the validator
	 * @return the earnings of {@code validator}
	 */
	@View BigInteger getStake(V validator);

	/**
	 * Yields the initial circulating supply of coins in the node.
	 * 
	 * @return the initial circulating supply
	 */
	@View BigInteger getInitialSupply();

	/**
	 * Yields the current circulating supply of coins in the node. This increases
	 * with time if inflation is not zero, since the gas used for the transactions
	 * gets inflated by inflation and distributed to the validators.
	 * 
	 * @return the current circulating supply
	 */
	@View BigInteger getCurrentSupply();

	/**
	 * Yields the final circulating supply of coins in the node.
	 * 
	 * @return the final circulating supply
	 */
	@View BigInteger getFinalSupply();

	/**
	 * Yields the initial circulating supply of red coins in the node.
	 * This does not change with the time.
	 * 
	 * @return the initial circulating supply of red coins
	 */
	@View BigInteger getInitialRedSupply();

	/**
	 * Yields the initial inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 1,000,000 means 1%.
	 * Inflation can be negative. For instance, -300,000 means -0.3%.
	 * 
	 * @return the initial inflation
	 */
	@View long getInitialInflation();

	/**
	 * Yields the current inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 1,000,000 means 1%.
	 * Inflation can be negative. For instance, -300,000 means -0.3%.
	 * This starts at {@link #getInitialInflation()} and decreases towards zero.
	 * 
	 * @return the current inflation
	 */
	@View long getCurrentInflation();

	/**
	 * Yields the percent of validators' rewards that gets staked. The rest is sent to the validators immediately.
	 * 1000000 = 1%.
	 * 
	 * @return the percent of validators' reward that gets staked
	 */
	@View int getPercentStaked();

	/**
	 * Yields the extra tax paid when a validator acquires the shares of another validator
	 * (in percent of the sale offer cost).
	 * 
	 * @return the extra tax paid. 1000000 means 1%
	 */
	@View int getBuyerSurcharge();

	/**
	 * Yields the slashing percent applied to stakes for each misbehavior.
	 * 
	 * @return the slashing percent. 1000000 means 1%
	 */
	@View int getSlashingForMisbehaving();

	/**
	 * Yields the slashing percent applied to stakes for no misbehavior (no vote).
	 * 
	 * @return the slashing percent. 1000000 means 1%
	 */
	@View int getSlashingForNotBehaving();

	/**
	 * Yields the amount of coins needed to start a new poll among the validators of this node.
	 * Both {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action)} and
	 * {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action, long, long)}
	 * require to pay this amount for starting a poll.
	 * 
	 * @return the amount of coins needed to start a new poll
	 */
	@View BigInteger getTicketForNewPoll();

	/**
	 * Yields a snapshot of the polls created among these validators,
	 * that have not been closed yet. Some of these polls might be over.
	 * These polls, typically, have as action the update of a consensus parameter.
	 * 
	 * @return the snapshot
	 */
	@View StorageSetView<Poll<V>> getPolls();

	/**
	 * Yields the number of rewards sent to this validators set.
	 * If this set is for a blockchain, this is typically the height of the blockchain.
	 * 
	 * @return the number of rewards sent to this validators set
	 */
	@View BigInteger getHeight();

	/**
	 * Yields the number of transactions validated with this validators set.
	 * 
	 * @return the number of transactions validated with this validators set
	 */
	@View BigInteger getNumberOfTransactions();

	/**
	 * Creates a new poll for the given action and adds it to those among these validators.
	 * Only a validator or the same manifest can start a poll among the validators.
	 * 
	 * @param amount the amount of coins payed to start the poll
	 * @param action the action of the poll
	 * @return the poll
	 */
	@Payable @FromContract SimplePoll<V> newPoll(BigInteger amount, SimplePoll.Action action);

	/**
	 * Creates a new poll with time window for the given action and adds it to those among these validators.
	 * Only a validator or the same manifest can start a poll among the validators.
	 * 
	 * @param amount the amount of coins payed to start the poll
	 * @param action the action of the poll
	 * @param start the starting moment of the poll, in milliseconds from now
	 * @param duration the duration of the poll, in milliseconds from the starting moment
	 * @return the poll
	 */
	@Payable @FromContract PollWithTimeWindow<V> newPoll(BigInteger amount, SimplePoll.Action action, long start, long duration);

	final class ValidatorSlashed<V extends Validator> extends Event {
		public final V validator;
		public final BigInteger amount;

		protected @FromContract ValidatorSlashed(V validator, BigInteger amount) {
			this.validator = validator;
			this.amount = amount;
		}
	}
}