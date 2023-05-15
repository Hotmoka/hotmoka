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

package io.takamaka.code.governance.tendermint;

import java.math.BigInteger;
import java.util.function.Function;

import io.takamaka.code.governance.AbstractValidators;
import io.takamaka.code.governance.Manifest;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;

/**
 * The validators of a Tendermint blockchain. They have an ED25519 public key
 * and an id derived from the public key, according to the algorithm used by Tendermint.
 */
public class TendermintValidators extends AbstractValidators<TendermintED25519Validator> {

	/**
	 * Creates a set of validators of a Tendermint blockchain.
	 * 
	 * @param manifest the manifest of the node having these validators
	 * @param validators the initial validators
	 * @param powers the initial powers of the initial validators
	 * @param ticketForNewPoll the amount of coins to pay for starting a new poll among the validators;
	 *                         both {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action)} and
	 *                         {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action, long, long)}
	 *                         require to pay this amount for starting a poll
	 * @param finalSupply the final supply of coins that will be reached, eventually
	 * @param initialInflation the initial inflation applied to the gas consumed by transactions before it gets sent
	 *                		   as reward to the validators. 0 means 0%, 1,000,000 means 1%,
	 *                  	   Inflation can be negative. For instance, -300,000 means -0.3%
	 * @param percentStaked the amount of rewards that gets staked. The rest is sent to the validators immediately.
	 *                      1000000 = 1%
	 * @param buyerSurcharge the extra tax paid when a validator acquires the shares of another validator
	 *                       (in percent of the offer cost). 1000000 = 1%
	 * @param slashingForMisbehaving the percent of stake that gets slashed for each misbehaving. 1000000 means 1%
	 * @param slashingForNotBehaving the percent of stake that gets slashed for not behaving (no vote). 1000000 means 1%
	 */
	private TendermintValidators(Manifest<TendermintED25519Validator> manifest, TendermintED25519Validator[] validators,
			BigInteger[] powers, BigInteger ticketForNewPoll, BigInteger finalSupply, long initialInflation,
			int percentStaked, int buyerSurcharge, int slashingForMisbehaving, int slashingForNotBehaving) {

		super(manifest, validators, powers, ticketForNewPoll, finalSupply, initialInflation, percentStaked,
				buyerSurcharge, slashingForMisbehaving, slashingForNotBehaving);
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, TendermintED25519Validator buyer, Offer<TendermintED25519Validator> offer) {
		// it is important to redefine this method, so that the same method with
		// argument of type PayableContract and Validator is redefined by the compiler with a bridge method
		// that casts the argument to TendermintED25519Validator and calls this method. In this way
		// only instances of TendermintED25519Validator can become shareholders (ie, actual validators)
		super.accept(amount, buyer, offer);
	}

	/**
	 * The builder  of a tendermint validators object.
	 */
	@Exported
	public static class Builder extends Storage implements Function<Manifest<TendermintED25519Validator>, TendermintValidators> {
		private final StorageList<TendermintED25519Validator> validators = new StorageLinkedList<>();
		private final StorageList<BigInteger> powers = new StorageLinkedList<>();
		private final BigInteger ticketForNewPoll;
		private final BigInteger finalSupply;
		private final long initialInflation;
		private final int percentStaked;
		private final int buyerSurcharge;
		private final int slashingForMisbehaving;
		private final int slashingForNotBehaving;

		/**
		 * Creates the builder of a set of validators of a Tendermint blockchain.
		 * 
		 * @param ticketForNewPoll the amount of coins to pay for starting a new poll among the validators;
		 *                         both {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action)} and
		 *                         {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action, long, long)}
		 *                         require to pay this amount for starting a poll
		 * @param finalSupply the final supply of coins that will be reached, eventually
		 * @param initialInflation the initial inflation applied to the gas consumed by transactions before it gets sent
		 *                		   as reward to the validators. 0 means 0%, 1,000,000 means 1%,
		 *                  	   Inflation can be negative. For instance, -300,000 means -0.3%
		 * @param percentStaked the amount of rewards that gets staked. The rest is sent to the validators immediately.
		 *                      1000000 = 1%
		 * @param buyerSurcharge the extra tax paid when a validator acquires the shares of another validator
		 *                       (in percent of the offer cost). 1000000 = 1%
		 * @param slashingForMisbehaving the percent of stake that gets slashed for each misbehaving. 1000000 means 1%
		 * @param slashingForNotBehaving the percent of stake that gets slashed for not behaving (no vote). 1000000 means 1%
		 */
		public Builder(BigInteger ticketForNewPoll, BigInteger finalSupply, long initialInflation,
				int percentStaked, int buyerSurcharge, int slashingForMisbehaving, int slashingForNotBehaving) {

			this.ticketForNewPoll = ticketForNewPoll;
			this.finalSupply = finalSupply;
			this.initialInflation = initialInflation;
			this.percentStaked = percentStaked;
			this.buyerSurcharge = buyerSurcharge;
			this.slashingForMisbehaving = slashingForMisbehaving;
			this.slashingForNotBehaving = slashingForNotBehaving;
		}

		/**
		 * Adds a new validator to this builder.
		 * 
		 * @param publicKey the public key of the validator
		 * @param power the power of the added validator
		 */
		public void addValidator(String publicKey, long power) {
			validators.add(new TendermintED25519Validator(publicKey));
			powers.add(BigInteger.valueOf(power));
		}

		@Override
		public TendermintValidators apply(Manifest<TendermintED25519Validator> manifest) {
			return new TendermintValidators(manifest, validators.toArray(TendermintED25519Validator[]::new),
				powers.toArray(BigInteger[]::new), ticketForNewPoll, finalSupply, initialInflation,
				percentStaked, buyerSurcharge, slashingForMisbehaving, slashingForNotBehaving);
		}
	}
}