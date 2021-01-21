package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.isSystemCall;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.dao.SimpleSharedEntity;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.system.poll.IncreaseVersionValidatorPoll;
import io.takamaka.code.system.poll.Poll;
import io.takamaka.code.system.poll.ValidatorPoll;

/**
 * A generic implementation of the validators.
 */
public class GenericValidators extends SimpleSharedEntity<SharedEntity.Offer> implements Validators {

	/**
	 * The manifest of the node having these validators.
	 */
	private final Manifest manifest;

	/**
	 * Creates the validators initialized with the given accounts.
	 * 
	 * @param manifest the manifest of the node
	 * @param validators the initial accounts
	 * @param powers the initial powers of the initial accounts; each refers
	 *               to the corresponding element of {@code validators}, hence
	 *               {@code validators} and {powers} have the same length
	 */
	protected GenericValidators(Manifest manifest, Validator[] validators, BigInteger[] powers) {
		super(validators, powers);

		this.manifest = manifest;
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
	private GenericValidators(Manifest manifest, String publicKeys, String powers) {
		this(manifest, buildValidators(publicKeys), buildPowers(powers));
	}

	private static Validator[] buildValidators(String publicKeysAsStringSequence) {
		return splitAtSpaces(publicKeysAsStringSequence).stream()
			.map(Validator::new)
			.toArray(Validator[]::new);
	}

	protected static BigInteger[] buildPowers(String powersAsStringSequence) {
		return splitAtSpaces(powersAsStringSequence).stream()
			.map(BigInteger::new)
			.toArray(BigInteger[]::new);
	}

	protected static List<String> splitAtSpaces(String s) {
		List<String> list = new ArrayList<>();
		int pos;
		while ((pos = s.indexOf(' ')) >= 0) {
			list.add(s.substring(0, pos));
			s = s.substring(pos + 1);
		}

		if (!s.isEmpty())
			list.add(s);

		return list;
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, Offer offer) {
		// we ensure that the only shareholders are Validator's
		require(caller() instanceof Validator, () -> "only a " + Validator.class.getSimpleName() + " can accept an offer");
		super.accept(amount, offer);
		event(new ValidatorsUpdate());
	}

	@Override
	public void reward(String behaving, String misbehaving, BigInteger gasConsumedForCpuOrStorage) {
		require(isSystemCall(), "the validators can only be rewarded with a system request");

		List<String> behavingIDs = splitAtSpaces(behaving);
		if (!behavingIDs.isEmpty()) {
			// compute the total power of the well behaving validators; this is always positive
			BigInteger totalPower = getShareholders()
				.filter(shareholder -> behavingIDs.contains(((Validator) shareholder).id()))
				.map(this::sharesOf)
				.reduce(ZERO, BigInteger::add);

			// distribute the balance of this contract to the well behaving validators, in proportion to their power
			final BigInteger balance = balance();
			getShareholders()
				.filter(shareholder -> behavingIDs.contains(((Validator) shareholder).id()))
				.forEachOrdered(shareholder -> shareholder.receive(balance.multiply(sharesOf(shareholder)).divide(totalPower)));
		}

		// the gas station is informed about the amount of gas consumed for CPU or storage, so that it can update the gas price
		manifest.gasStation.takeNoteOfGasConsumedDuringLastReward(gasConsumedForCpuOrStorage);
	}

	@Exported
	public static class Builder extends Storage implements Function<Manifest, Validators> {
		private final String publicKeys;
		private final String powers;

		public Builder(String publicKeys, String powers) {
			this.publicKeys = publicKeys;
			this.powers = powers;
		}

		@Override
		public Validators apply(Manifest manifest) {
			return new GenericValidators(manifest, publicKeys, powers);
		}
	}
	
	public Poll newPoll() {
		return new IncreaseVersionValidatorPoll(manifest);
	}
}