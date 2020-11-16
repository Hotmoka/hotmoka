package io.takamaka.code.system;

import java.math.BigInteger;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.internal.ModifiableStorageMapImpl;

public class Validators extends SharedEntity {

	/**
	 * The current validators, organized as a map from their unique
	 * identifier to the validator with that identifier.
	 */
	private final ModifiableStorageMapImpl<String, Validator> validators = new ModifiableStorageMapImpl<>();

	/**
	 * Creates a set of validators initialized with the given validators.
	 * 
	 * @param validators the initial validators in the set
	 */
	Validators(Validator[] validators, BigInteger[] powers) {
		super(validators, powers);
	}

	/**
	 * Yields a space separated concatenation of secret, type and power of each validator.
	 * 
	 * @return the concatenation
	 */
	@Override
	public @View String toString() {
		return "";
		/*return power.stream().filter(entry -> entry.getValue().signum() > 0)
			.map(StorageMap.Entry::getKey)
			.filter(Validator::isRevealed)
			.map(Validator::toString)
			.collect(Collectors.joining(" "));*/
	}

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
	 */
	public void reward(String behaving, String misbehaving) {
	}
}