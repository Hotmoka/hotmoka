package io.takamaka.code.system;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.util.StorageArray;
import io.takamaka.code.util.StorageMap;

public class Validators extends Storage {

	/**
	 * The current validators, organized as a map from their unique
	 * identifier to the validator with that identifier.
	 */
	private final StorageMap<String, Validator> validators = new StorageMap<>();

	/**
	 * Creates a set of validators initialized with the given validators.
	 * 
	 * @param validators the initial validators in the set
	 */
	Validators(StorageArray<Validator> validators) {
		for (Validator validator: validators)
			if (this.validators.putIfAbsent(validator.id, validator) != null)
				throw new IllegalArgumentException("reapeated validator identifier " + validator.id);
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
	 * @param behaving a space-separated identifiers of validators that behaved correctly
	 * @param misbehaving a space-separated identifiers of validators that misbehaved
	 */
	public void reward(String behaving, String misbehaving) {
	}
}