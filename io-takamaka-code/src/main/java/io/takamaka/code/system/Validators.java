package io.takamaka.code.system;

import java.math.BigInteger;

import io.takamaka.code.dao.SharedEntity;

/**
 * The validators are the accounts that get rewarded at specific
 * intervals, for instance when a new block is committed in a blockchain.
 * Any update to the number or properties of the validators must generate
 * an event of type {@link ValidatorsUpdate}.
 */
public interface Validators extends SharedEntity<SharedEntity.Offer> {

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
	 * @param behaving space-separated identifiers of validators that behaved correctly
	 * @param misbehaving space-separated identifiers of validators that misbehaved
	 * @param gasConsumedForCpuOrStorage the gas consumed for CPU usage or storage by the transactions
	 *                                   executed since the previous reward
	 */
	void reward(String behaving, String misbehaving, BigInteger gasConsumedForCpuOrStorage);
}