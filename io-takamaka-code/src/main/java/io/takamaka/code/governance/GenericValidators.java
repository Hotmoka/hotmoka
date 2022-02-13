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
import java.util.function.Function;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;

/**
 * A generic implementation of the validators. They are fixed to be instances of {@link Validator}.
 */
public class GenericValidators extends AbstractValidators<Validator> {

	/**
	 * Creates the validators initialized with the given accounts.
	 * 
	 * @param manifest the manifest of the node
	 * @param validators the initial accounts
	 * @param powers the initial powers of the initial accounts; each refers
	 *               to the corresponding element of {@code validators}, hence
	 *               {@code validators} and {powers} have the same length
	 * @param ticketForNewPoll the amount of coins to pay for starting a new poll among the validators;
	 *                         both {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action)} and
	 *                         {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action, long, long)}
	 *                         require to pay this amount for starting a poll
	 * @param finalSupply the final supply of coins that will be reached, eventually
	 * @param initialInflation the initial inflation applied to the gas consumed by transactions before it gets sent
	 *                		   as reward to the validators. 1,000,000 means 1%.
	 *                         Inflation can be negative. For instance, -300,000 means -0.3%
	 */
	protected GenericValidators(Manifest<Validator> manifest, Validator[] validators, BigInteger[] powers, BigInteger ticketForNewPoll, BigInteger finalSupply, long initialInflation) {
		super(manifest, validators, powers, ticketForNewPoll, finalSupply, initialInflation);
	}

	/**
	 * Creates the validators, from their public keys and powers.
	 *
	 * @param manifest the manifest of the node
	 * @param publicKeys the public keys of the initial validators,
	 *                   as a space-separated sequence of Base64-encoded public keys
	 * @param powers the initial powers of the initial validators,
	 *               as a space-separated sequence of integers; they must be as many
	 *               as there are public keys in {@code publicKeys}
	 * @param ticketForNewPoll the amount of coins to pay for starting a new poll among the validators;
	 *                         both {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action)} and
	 *                         {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action, long, long)}
	 *                         require to pay this amount for starting a poll
	 * @param finalSupply the final supply of coins that will be reached, eventually
	 * @param initialInflation the initial inflation applied to the gas consumed by transactions before it gets sent
	 *                		   as reward to the validators. 1,000,000 means 1%.
	 *                         Inflation can be negative. For instance, -300,000 means -0.3%
	 */
	private GenericValidators(Manifest<Validator> manifest, String publicKeys, String powers, BigInteger ticketForNewPoll, BigInteger finalSupply, long initialInflation) {
		this(manifest, buildValidators(publicKeys), buildPowers(powers), ticketForNewPoll, finalSupply, initialInflation);
	}

	private static Validator[] buildValidators(String publicKeysAsStringSequence) {
		return splitAtSpaces(publicKeysAsStringSequence).stream()
			.map(Validator::new)
			.toArray(Validator[]::new);
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, Validator buyer, Offer<Validator> offer) {
		// it is important to redefine this method, so that the same method with
		// argument of type PayableContract is redefined by the compiler with a bridge method
		// that casts the argument to Validator and calls this method. In this way
		// only instances of Validator can become shareholders (ie, actual validators)
		super.accept(amount, buyer, offer);
	}

	@Exported
	public static class Builder extends Storage implements Function<Manifest<Validator>, GenericValidators> {
		private final String publicKeys;
		private final String powers;
		private final BigInteger ticketForNewPoll;
		private final BigInteger finalSupply;
		private final long initialInflation;

		public Builder(String publicKeys, String powers, BigInteger ticketForNewPoll, BigInteger finalSupply, long initialInflation) {
			this.publicKeys = publicKeys;
			this.powers = powers;
			this.ticketForNewPoll = ticketForNewPoll;
			this.finalSupply = finalSupply;
			this.initialInflation = initialInflation;
		}

		@Override
		public GenericValidators apply(Manifest<Validator> manifest) {
			return new GenericValidators(manifest, publicKeys, powers, ticketForNewPoll, finalSupply, initialInflation);
		}
	}
}