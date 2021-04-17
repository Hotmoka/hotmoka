package io.takamaka.code.governance;

import java.math.BigInteger;

import io.takamaka.code.dao.Poll;
import io.takamaka.code.dao.PollWithTimeWindow;
import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.dao.SharedEntity.Offer;
import io.takamaka.code.dao.SimplePoll;
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
	 * @param behaving space-separated identifiers of validators that behaved correctly
	 * @param misbehaving space-separated identifiers of validators that misbehaved
	 * @param gasConsumed the gas consumed for CPU or RAM usage or storage by the transactions
	 *                    executed since the previous reward
	 * @param numberOfTransactionsSinceLastReward the number of transactions executed since
	 *                                            the previous reward
	 */
	@FromContract @Payable void reward(BigInteger amount, String behaving, String misbehaving, BigInteger gasConsumed, BigInteger numberOfTransactionsSinceLastReward);

	/**
	 * Yields the amount of coins needed to start a new poll among the validators of this node.
	 * Both {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action)} and
	 * {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action, long, long)}
	 * require to pay this amount for starting a poll.
	 */
	@View BigInteger getTicketForNewPoll();

	/**
	 * Yields a snapshot of the polls created among these validators,
	 * that have not been closed yet. Some of these polls might be over.
	 * These polls, typically, have as action the update of a consensus parameter.
	 */
	@View StorageSetView<Poll<V>> getPolls();

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
}