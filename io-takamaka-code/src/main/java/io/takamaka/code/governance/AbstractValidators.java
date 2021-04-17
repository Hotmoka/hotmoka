package io.takamaka.code.governance;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.isSystemCall;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.takamaka.code.dao.Poll;
import io.takamaka.code.dao.PollWithTimeWindow;
import io.takamaka.code.dao.SharedEntity.Offer;
import io.takamaka.code.dao.SimplePoll;
import io.takamaka.code.dao.SimpleSharedEntity;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageSet;
import io.takamaka.code.util.StorageSetView;
import io.takamaka.code.util.StorageTreeSet;

/**
 * A partial implementation of the validators.
 * 
 * @param <V> the type of the validator contracts
 */
public abstract class AbstractValidators<V extends Validator> extends SimpleSharedEntity<V, Offer<V>> implements Validators<V> {

	/**
	 * The manifest of the node having these validators.
	 */
	private final Manifest<V> manifest;

	/**
	 * The amount of coins to pay for starting a new poll among the validators.
	 */
	private final BigInteger ticketForNewPoll;

	/**
	 * The number of transactions validated up to now.
	 * Note that this is updated at each reward.
	 */
	private BigInteger numberOfTransactions;

	/**
	 * The number of rewards that have been sent to the validators.
	 * If the node is a blockchain, this is typically the height of the blockchain.
	 */
	private BigInteger height;

	/**
	 * The polls created among the validators of this manifest, that have not been closed yet.
	 * Some of these polls might be over.
	 */
	private final StorageSet<Poll<V>> polls = new StorageTreeSet<>();

	/**
	 * A snapshot of the current value of {@link #polls}.
	 */
	private StorageSetView<Poll<V>> snapshotOfPolls;

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
	protected AbstractValidators(Manifest<V> manifest, V[] validators, BigInteger[] powers, BigInteger ticketForNewPoll) {
		super(validators, powers);

		require(ticketForNewPoll != null, "the ticket for new poll must be non-null");
		require(ticketForNewPoll.signum() >= 0, "the ticket for new poll must be non-negative");

		this.manifest = manifest;
		this.ticketForNewPoll = ticketForNewPoll;
		this.numberOfTransactions = ZERO;
		this.height = ZERO;
		this.snapshotOfPolls = polls.snapshot();
	}

	@Override
	public final @View BigInteger getTicketForNewPoll() {
		return ticketForNewPoll;
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
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, V buyer, Offer<V> offer) {
		// it is important to redefine this method, so that the same method with
		// argument of type PayableContract is redefined by the compiler with a bridge method
		// that casts the argument to Validator and calls this method. In this way
		// only instances of Validator can become shareholders (ie, actual validators)
		super.accept(amount, buyer, offer);
		event(new ValidatorsUpdate());
	}

	@Override
	@FromContract @Payable public void reward(BigInteger amount, String behaving, String misbehaving, BigInteger gasConsumed, BigInteger numberOfTransactionsSinceLastReward) {
		require(isSystemCall(), "the validators can only be rewarded with a system request");

		List<String> behavingIDs = splitAtSpaces(behaving);
		if (!behavingIDs.isEmpty()) {
			// compute the total power of the well behaving validators; this is always positive
			BigInteger totalPower = getShareholders()
				.filter(shareholder -> behavingIDs.contains(shareholder.id()))
				.map(this::sharesOf)
				.reduce(ZERO, BigInteger::add);

			// distribute the balance of this contract to the well behaving validators, in proportion to their power
			final BigInteger balance = balance();
			getShareholders()
				.filter(shareholder -> behavingIDs.contains(shareholder.id()))
				.forEachOrdered(shareholder -> shareholder.receive(balance.multiply(sharesOf(shareholder)).divide(totalPower)));
		}

		// the gas station is informed about the amount of gas consumed for CPU or storage, so that it can update the gas price
		manifest.gasStation.takeNoteOfGasConsumedDuringLastReward(gasConsumed);

		// we increase the number of rewards (ie, the height of the blockchain, if the node is part of a blockchain)
		// but only if there are transactions, which gives to the underlying blockchain engine the possibility
		// to stop generating empty blocks
		if (numberOfTransactionsSinceLastReward.signum() > 0) {
			height = height.add(ONE);

			// we add to the cumulative number of transactions validated up to now
			numberOfTransactions = numberOfTransactions.add(numberOfTransactionsSinceLastReward);
		}
	}

	@Override
	@Payable @FromContract
	public final SimplePoll<V> newPoll(BigInteger amount, SimplePoll.Action action) {
		require(amount.compareTo(ticketForNewPoll) >= 0, () -> "a new poll costs " + ticketForNewPoll + " coins");
		checkThatItCanStartPoll(caller());

		SimplePoll<V> poll = new SimplePoll<>(this, action) {

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
	public final PollWithTimeWindow<V> newPoll(BigInteger amount, SimplePoll.Action action, long start, long duration) {
		require(amount.compareTo(ticketForNewPoll) >= 0, () -> "a new poll costs " + ticketForNewPoll + " coins");
		checkThatItCanStartPoll(caller());

		PollWithTimeWindow<V> poll = new PollWithTimeWindow<>(this, action, start, duration) {
	
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
	public final @View StorageSetView<Poll<V>> getPolls() {
		return snapshotOfPolls;
	}

	@Override
	public final @View BigInteger getHeight() {
		return height;
	}

	@Override
	public final @View BigInteger getNumberOfTransactions() {
		return numberOfTransactions;
	}

	private void addPoll(SimplePoll<V> poll) {
		polls.add(poll);
		snapshotOfPolls = polls.snapshot();
	}

	private void removePoll(SimplePoll<V> poll) {
		polls.remove(poll);
		snapshotOfPolls = polls.snapshot();
	}

	private void checkThatItCanStartPoll(Contract caller) {
		require(isShareholder(caller) || caller == manifest || caller == manifest.versions || caller == manifest.gasStation,
			"only a validator or the same manifest can start a poll among the validators");
	}
}