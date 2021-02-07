package io.takamaka.code.system.tendermint;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;
import java.util.function.Function;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.system.GenericValidators;
import io.takamaka.code.system.Manifest;
import io.takamaka.code.system.Validator;
import io.takamaka.code.system.Validators;

/**
 * The validators of a Tendermint blockchain. They have an ED25519 public key
 * and an id derived from the public key, according to the algorithm used by Tendermint.
 */
public class TendermintValidators extends GenericValidators {

	/**
	 * Creates a set of validators of aTendermint blockchain, from their public keys and powers.
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
	private TendermintValidators(Manifest manifest, String publicKeys, String powers, BigInteger ticketForNewPoll) {
		super(manifest, buildValidators(publicKeys), buildPowers(powers), ticketForNewPoll);
	}

	private static TendermintED25519Validator[] buildValidators(String publicKeysAsStringSequence) {
		return splitAtSpaces(publicKeysAsStringSequence).stream()
			.map(TendermintED25519Validator::new)
			.toArray(TendermintED25519Validator[]::new);
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, Validator buyer, Offer<Validator> offer) {
		// we ensure that the only shareholders are Validator's
		require(caller() instanceof TendermintED25519Validator, () -> "only a " + TendermintED25519Validator.class.getSimpleName() + " can accept an offer");
		super.accept(amount, buyer, offer);
	}

	@Exported
	public static class Builder extends Storage implements Function<Manifest, Validators> {
		private final String publicKeys;
		private final String powers;
		private final BigInteger ticketForNewPoll;

		public Builder(String publicKeys, String powers, BigInteger ticketForNewPoll) {
			this.publicKeys = publicKeys;
			this.powers = powers;
			this.ticketForNewPoll = ticketForNewPoll;
		}

		@Override
		public Validators apply(Manifest manifest) {
			return new TendermintValidators(manifest, publicKeys, powers, ticketForNewPoll);
		}
	}
}