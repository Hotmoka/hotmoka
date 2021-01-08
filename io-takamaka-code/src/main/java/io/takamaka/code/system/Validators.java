package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.isSystemCall;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;

/**
 * The validators are the accounts that get rewarded at specific
 * intervals, for instance when a new block is committed in a blockchain.
 */
public class Validators extends SharedEntity<SharedEntity.Offer> {

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
	protected Validators(Manifest manifest, Validator[] validators, BigInteger[] powers) {
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
	private Validators(Manifest manifest, String publicKeys, String powers) {
		this(manifest, buildValidators(publicKeys), buildPowers(powers));
	}

	protected static Validator[] buildValidators(String publicKeysAsStringSequence) {
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
	}

	private int counter; // TODO: remove at the end

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

		// the gas station is informed about the amount of gas consumed for CPU, so that
		// it can update the gas price
		manifest.gasStation.takeNoteOfGasConsumedDuringLastReward(gasConsumedForCpuOrStorage);

		if (++counter % 20 == 0) // TODO: remove at the end
			manifest.versions.increaseVerificationVersion();
	}

	@Exported
	public static class Builder extends Storage implements Function<Manifest, Validators> {
		protected final String publicKeys;
		protected final String powers;

		public Builder(String publicKeys, String powers) {
			this.publicKeys = publicKeys;
			this.powers = powers;
		}

		@Override
		public Validators apply(Manifest manifest) {
			return new Validators(manifest, publicKeys, powers);
		}
	}
}