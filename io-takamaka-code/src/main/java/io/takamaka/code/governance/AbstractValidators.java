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
import io.takamaka.code.lang.Account;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageSet;
import io.takamaka.code.util.StorageSetView;
import io.takamaka.code.util.StorageTreeMap;
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
	 * The earnings of each validators, that have not yet been sent to the validators.
	 * They are not given immediately to the validators,
	 * but rather stored in this map and given only if a validator sells all its shares.
	 */
	private final StorageMap<V, BigInteger> stakes = new StorageTreeMap<>();

	/**
	 * The amount of rewards that get staked. The remaining percent is sent
	 * to the validators immediately.
	 */
	private final int percentStaked = 75;

	/**
	 * Extra tax paid when a validator acquires the shares of another validator
	 * (in percent of the offer cost).
	 */
	private final int buyerSurcharge = 50;

	/**
	 * The amount of coins to pay for starting a new poll among the validators.
	 */
	private final BigInteger ticketForNewPoll;

	/**
	 * The initial circulating supply of coins in the node.
	 */
	private final BigInteger initialSupply;

	/**
	 * The current circulating supply of coins in the node. This increases
	 * with time if inflation is not zero, since the gas used for the transactions
	 * gets inflated by inflation and distributed to the validators. This is
	 * between {@link #initialSupply} and {@link #finalSupply}.
	 */
	private BigInteger currentSupply;

	/**
	 * The final circulating supply of coins in the node, that will be reached
	 * eventually, if inflation is not zero.
	 */
	private final BigInteger finalSupply;

	/**
	 * The initial circulating supply of red coins in the node.
	 * This does not change with the time.
	 */
	private final BigInteger initialRedSupply;

	/**
	 * The initial inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 0 means 0%, 100,000 means 1%,
	 * 10,000,000 means 100%, 20,000,000 means 200% and so on.
	 * Inflation can be negative. For instance, -30,000 means -0.3%.
	 * This defaults to 10,000 (that is, inflation is 0.1% by default).
	 */
	private final long initialInflation;

	/**
	 * The current inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 0 means 0%, 100,000 means 1%,
	 * 10,000,000 means 100%, 20,000,000 means 200% and so on.
	 * Inflation can be negative. For instance, -30,000 means -0.3%.
	 * This starts at {@link #initialInflation} and goes towards zero.
	 */
	private long currentInflation;

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
	 * @param finalSupply the final supply of coins that will be reached, eventually
	 * @param initialInflation the initial inflation applied to the gas consumed by transactions before it gets sent
	 *                		   as reward to the validators. 0 means 0%, 100,000 means 1%,
	 *                  	   10,000,000 means 100%, 20,000,000 means 200% and so on.
	 *                  	   Inflation can be negative. For instance, -30,000 means -0.3%
	 */
	protected AbstractValidators(Manifest<V> manifest, V[] validators, BigInteger[] powers, BigInteger ticketForNewPoll, BigInteger finalSupply, long initialInflation) {
		super(validators, powers);

		require(ticketForNewPoll != null, "the ticket for new poll must be non-null");
		require(ticketForNewPoll.signum() >= 0, "the ticket for new poll must be non-negative");

		this.manifest = manifest;
		Account gamete = manifest.getGamete();
		this.currentSupply = gamete.balance(); // initially, all coins are inside the gamete
		this.initialSupply = currentSupply;
		this.finalSupply = finalSupply;
		this.initialRedSupply = gamete.balanceRed();
		this.initialInflation = initialInflation;
		this.currentInflation = initialInflation;
		this.ticketForNewPoll = ticketForNewPoll;
		this.numberOfTransactions = ZERO;
		this.height = ZERO;
		this.snapshotOfPolls = polls.snapshot();
		for (V validator: validators)
			stakes.put(validator, BigInteger.ZERO);
	}

	@Override
	public final BigInteger getStake(V validator) {
		return stakes.getOrDefault(validator, BigInteger.ZERO);
	}

	@Override
	public final BigInteger getInitialSupply() {
		return initialSupply;
	}

	@Override
	public final BigInteger getCurrentSupply() {
		return currentSupply;
	}

	@Override
	public final BigInteger getFinalSupply() {
		return finalSupply;
	}

	@Override
	public final BigInteger getInitialRedSupply() {
		return initialRedSupply;
	}

	@Override
	public @View long getInitialInflation() {
		return initialInflation;
	}

	@Override
	public @View long getCurrentInflation() {
		return currentInflation;
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

		BigInteger costWithSurchage = offer.cost.multiply(BigInteger.valueOf(buyerSurcharge + 100)).divide(BigInteger.valueOf(100L));
		require(costWithSurchage.compareTo(amount) <= 0, "not enough money to accept the offer: you need " + costWithSurchage);
		super.accept(amount, buyer, offer);

		// if the seller is not a validator anymore, we send its staked coins
		V seller = offer.seller;
		if (sharesOf(seller).signum() == 0) {
			seller.receive(stakes.get(seller));
			stakes.remove(seller);
		}

		event(new ValidatorsUpdate());
	}

	@Override
	@FromContract @Payable public void reward(BigInteger amount, BigInteger minted, String behaving, String misbehaving, BigInteger gasConsumed, BigInteger numberOfTransactionsSinceLastReward) {
		require(isSystemCall(), "the validators can only be rewarded with a system request");

		List<String> behavingIDs = splitAtSpaces(behaving);
		if (!behavingIDs.isEmpty()) {
			// compute the total power of the well behaving validators; this is always positive
			BigInteger totalPower = getShareholders()
				.filter(validator -> behavingIDs.contains(validator.id()))
				.map(this::sharesOf)
				.reduce(ZERO, BigInteger::add);

			// compute the total amount of staked coins
			BigInteger totalStaked = stakes.values().reduce(BigInteger.ZERO, BigInteger::add);

			// compute the balance that is not staked and must be distributed
			BigInteger toDistribute = balance().subtract(totalStaked);

			// 75% of the distribution gets staked for the well-behaving validators, in proportion to their power
			final BigInteger addedToStakes = toDistribute.multiply(BigInteger.valueOf(percentStaked)).divide(BigInteger.valueOf(100L));
			getShareholders()
				.filter(validator -> behavingIDs.contains(validator.id()))
				.forEachOrdered(validator -> stakes.update(validator, old -> old.add(addedToStakes.multiply(sharesOf(validator)).divide(totalPower))));

			// distribute immediately the remaining 25% to the well-behaving validators, in proportion to their power
			final BigInteger paid = toDistribute.subtract(addedToStakes);
			getShareholders()
				.filter(validator -> behavingIDs.contains(validator.id()))
				.forEachOrdered(validator -> validator.receive(paid.multiply(sharesOf(validator)).divide(totalPower)));
		}

		// TODO: slash staked coins for misbehaving validators

		// the gas station is informed about the amount of gas consumed for CPU, RAM or storage, so that it can update the gas price
		manifest.gasStation.takeNoteOfGasConsumedDuringLastReward(gasConsumed);

		// we increase the number of rewards (ie, the height of the blockchain, if the node is part of a blockchain)
		// but only if there are transactions, which gives to the underlying blockchain engine the possibility
		// to stop generating empty blocks
		if (numberOfTransactionsSinceLastReward.signum() > 0) {
			height = height.add(ONE);

			// we add to the cumulative number of transactions validated up to now
			numberOfTransactions = numberOfTransactions.add(numberOfTransactionsSinceLastReward);

			// the total supply is increased by the coins minted since the previous reward
			currentSupply = currentSupply.add(minted);

			// we compute the current inflation, so that it approaches zero while
			// the current supply is reaching the final supply
			BigInteger delta = finalSupply.subtract(initialSupply);
			if (delta.signum() != 0) {
				BigInteger currentDelta = finalSupply.subtract(currentSupply);
				long oldCurrentInflation = currentInflation;

				// if the current supply reached the total supply, inflation is forced to zero
				if (delta.signum() <= 0 && currentDelta.signum() >= 0)
					currentInflation = 0L;
				else if (delta.signum() >= 0 && currentDelta.signum() <= 0)
					currentInflation = 0L;
				else
					currentInflation = BigInteger.valueOf(initialInflation).multiply(currentDelta).divide(delta).longValue();

				if (currentInflation != oldCurrentInflation)
					event(new InflationUpdate(currentInflation));
			}
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