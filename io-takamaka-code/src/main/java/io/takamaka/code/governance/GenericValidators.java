package io.takamaka.code.governance;

import static io.takamaka.code.lang.Takamaka.event;

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
	 */
	protected GenericValidators(Manifest<Validator> manifest, Validator[] validators, BigInteger[] powers, BigInteger ticketForNewPoll) {
		super(manifest, validators, powers, ticketForNewPoll);
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
	 */
	private GenericValidators(Manifest<Validator> manifest, String publicKeys, String powers, BigInteger ticketForNewPoll) {
		this(manifest, buildValidators(publicKeys), buildPowers(powers), ticketForNewPoll);
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
		event(new ValidatorsUpdate());
	}

	@Exported
	public static class Builder extends Storage implements Function<Manifest<Validator>, GenericValidators> {
		private final String publicKeys;
		private final String powers;
		private final BigInteger ticketForNewPoll;

		public Builder(String publicKeys, String powers, BigInteger ticketForNewPoll) {
			this.publicKeys = publicKeys;
			this.powers = powers;
			this.ticketForNewPoll = ticketForNewPoll;
		}

		@Override
		public GenericValidators apply(Manifest<Validator> manifest) {
			return new GenericValidators(manifest, publicKeys, powers, ticketForNewPoll);
		}
	}
}