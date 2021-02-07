package io.takamaka.code.system.tendermint;

import java.math.BigInteger;
import java.util.function.Function;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.system.AbstractValidators;
import io.takamaka.code.system.Manifest;

/**
 * The validators of a Tendermint blockchain. They have an ED25519 public key
 * and an id derived from the public key, according to the algorithm used by Tendermint.
 */
public class TendermintValidators extends AbstractValidators<TendermintED25519Validator> {

	/**
	 * Creates a set of validators of a Tendermint blockchain, from their public keys and powers.
	 * 
	 * @param manifest the manifest of the node having these validators
	 * @param publicKeys the public keys of the initial validators, as a space-separated
	 *                   sequence of Base64-encoded ED25519 publicKeys
	 * @param powers the initial powers of the initial validators, as a space-separated sequence of integers;
	 *               they must be as many as there are public keys in {@code publicKeys}
	 * @param ticketForNewPoll the amount of coins to pay for starting a new poll among the validators;
	 *                         both {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action)} and
	 *                         {@link #newPoll(BigInteger, io.takamaka.code.dao.SimplePoll.Action, long, long)}
	 *                         require to pay this amount for starting a poll
	 */
	private TendermintValidators(Manifest<TendermintED25519Validator> manifest, String publicKeys, String powers, BigInteger ticketForNewPoll) {
		super(manifest, buildValidators(publicKeys), buildPowers(powers), ticketForNewPoll);
	}

	private static TendermintED25519Validator[] buildValidators(String publicKeysAsStringSequence) {
		return splitAtSpaces(publicKeysAsStringSequence).stream()
			.map(TendermintED25519Validator::new)
			.toArray(TendermintED25519Validator[]::new);
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, TendermintED25519Validator buyer, Offer<TendermintED25519Validator> offer) {
		// it is important to redefine this method, so that the same method with
		// argument of type PayableContract is redefined by the compiler with a bridge method
		// that casts the argument to TendermintED25519Validator and calls this method. In this way
		// only instances of TendermintED25519Validator can become shareholders (ie, actual validators)
		super.accept(amount, buyer, offer);
	}

	@Exported
	public static class Builder extends Storage implements Function<Manifest<TendermintED25519Validator>, TendermintValidators> {
		private final String publicKeys;
		private final String powers;
		private final BigInteger ticketForNewPoll;

		public Builder(String publicKeys, String powers, BigInteger ticketForNewPoll) {
			this.publicKeys = publicKeys;
			this.powers = powers;
			this.ticketForNewPoll = ticketForNewPoll;
		}

		@Override
		public TendermintValidators apply(Manifest<TendermintED25519Validator> manifest) {
			return new TendermintValidators(manifest, publicKeys, powers, ticketForNewPoll);
		}
	}
}