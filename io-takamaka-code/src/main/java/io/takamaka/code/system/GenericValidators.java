package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.isSystemCall;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.takamaka.code.dao.Poll;
import io.takamaka.code.dao.PollWithTimeWindow;
import io.takamaka.code.dao.SharedEntity3.Offer;
import io.takamaka.code.dao.SimplePoll;
import io.takamaka.code.dao.SimpleSharedEntity3;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageSet;
import io.takamaka.code.util.StorageSetView;
import io.takamaka.code.util.StorageTreeSet;

/**
 * A generic implementation of the validators.
 */
public class GenericValidators extends SimpleSharedEntity3<Validator, Offer<Validator>> implements Validators {

	/**
	 * The manifest of the node having these validators.
	 */
	private final Manifest manifest;

	/**
	 * The amount of coins to pay for starting a new poll among the validators.
	 */
	private final BigInteger ticketForNewPoll;

	/**
	 * The polls created among the validators of this manifest, that have not been closed yet.
	 * Some of these polls might be over.
	 */
	private final StorageSet<Poll<Validator>> polls = new StorageTreeSet<>();

	/**
	 * A snapshot of the current value of {@link #polls}.
	 */
	private StorageSetView<Poll<Validator>> snapshotOfPolls;

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
	protected GenericValidators(Manifest manifest, Validator[] validators, BigInteger[] powers, BigInteger ticketForNewPoll) {
		super(validators, powers);

		require(ticketForNewPoll != null, "the ticket for new poll must be non-null");
		require(ticketForNewPoll.signum() >= 0, "the ticket for new poll must be non-negative");

		this.manifest = manifest;
		this.ticketForNewPoll = ticketForNewPoll;
		this.snapshotOfPolls = polls.snapshot();
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
	private GenericValidators(Manifest manifest, String publicKeys, String powers, BigInteger ticketForNewPoll) {
		this(manifest, buildValidators(publicKeys), buildPowers(powers), ticketForNewPoll);
	}

	@Override
	public final @View BigInteger getTicketForNewPoll() {
		return ticketForNewPoll;
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
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, Validator buyer, Offer<Validator> offer) {
		// it is important to redefine this method, so that the same method with
		// argument of type PayableContract is redefined by the compiler with a bridge method
		// that casts the argument to Validator and calls this method. In this way
		// only instances of Validator can become shareholders (ie, actual validators)
		super.accept(amount, buyer, offer);
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

	@Override
	@Payable @FromContract
	public final SimplePoll<Validator> newPoll(BigInteger amount, SimplePoll.Action action) {
		require(amount.compareTo(ticketForNewPoll) >= 0, () -> "a new poll costs " + ticketForNewPoll + " coins");
		checkThatItCanStartPoll(caller());

		SimplePoll<Validator> poll = new SimplePoll<Validator>(this, action) {

			@Override
			public void close() {
				super.close();
				removePoll(this);
			}
		};
	
		addPoll(poll);

		return poll;
	}

	@Override
	@Payable @FromContract
	public final PollWithTimeWindow<Validator> newPoll(BigInteger amount, SimplePoll.Action action, long start, long duration) {
		require(amount.compareTo(ticketForNewPoll) >= 0, () -> "a new poll costs " + ticketForNewPoll + " coins");
		checkThatItCanStartPoll(caller());

		PollWithTimeWindow<Validator> poll = new PollWithTimeWindow<Validator>(this, action, start, duration) {
	
			@Override
			public void close() {
				super.close();
				removePoll(this);
			}
		};
	
		addPoll(poll);
	
		return poll;
	}

	@Override
	public final @View StorageSetView<Poll<Validator>> getPolls() {
		return snapshotOfPolls;
	}

	private void addPoll(SimplePoll<Validator> poll) {
		polls.add(poll);
		snapshotOfPolls = polls.snapshot();
	}

	private void removePoll(SimplePoll<Validator> poll) {
		polls.remove(poll);
		snapshotOfPolls = polls.snapshot();
	}

	private void checkThatItCanStartPoll(Contract caller) {
		require(isShareholder(caller) || caller == manifest || caller == manifest.versions || caller == manifest.gasStation,
			"only a validator or the same manifest can start a poll among the validators");
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
			return new GenericValidators(manifest, publicKeys, powers, ticketForNewPoll);
		}
	}
}