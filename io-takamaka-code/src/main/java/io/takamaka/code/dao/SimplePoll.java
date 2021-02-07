package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageMapView.Entry;
import io.takamaka.code.util.StorageTreeMap;

/**
 * A poll among the shareholders of a shared entity. Each shareholder can vote with a number of votes
 * between one and its shares at the time of creation of the poll.
 * Once the goal for the poll is reached, an action is run.
 */
@Exported
public class SimplePoll<Voter extends PayableContract> extends Storage implements Poll<Voter> {

	/**
	 * An action that is triggered if the goal of the poll has been reached.
	 */
	public static abstract class Action extends Storage {
		protected abstract @View String getDescription();
		protected abstract void run();
	}

	/** 
	 * The eligible voters, with the maximal amount of votes they can cast.
	 */
	private final StorageMapView<Voter, BigInteger> eligibleVoters;

	/**
	 * The voters up to now, with the votes that each of them has cast.
	 */
	private final StorageMap<Voter, BigInteger> votersUpToNow = new StorageTreeMap<>();

	/**
	 * The action run if the goal of the poll is reached.
	 */
	private final Action action;

	/**
	 * The maximal amount of votes that can be cast for this poll.
	 */
	private final BigInteger totalVotesCastable;

	/**
	 * A snapshot of the current {@link #votersUpToNow}.
	 */
	private StorageMapView<Voter, BigInteger> snapshotOfVotersUpToNow;

	/**
	 * The votes cast up to now.
	 */
	private BigInteger votesCastUpToNow;

	/**
	 * True if and only if this poll has been closed.
	 */
	private boolean isClosed;
	
	public SimplePoll(SharedEntity<Voter, ?> shareholders, Action action) {
		require(shareholders != null, "the shareholders cannot be null");
		require(action != null, "the action cannot be null");

		this.eligibleVoters = shareholders.getShares();
		this.totalVotesCastable = eligibleVoters.stream().map(Entry::getValue).reduce(ZERO, BigInteger::add);
		this.action = action;
		this.snapshotOfVotersUpToNow = votersUpToNow.snapshot();
		this.votesCastUpToNow = ZERO;
	}

	@Override @View
	public final String getDescription() {
		return action.getDescription();
	}

	@Override @View
	public final StorageMapView<Voter, BigInteger> getEligibleVoters() {
		return eligibleVoters;
	}

	@Override @View
	public final StorageMapView<Voter, BigInteger> getVotersUpToNow() {
		return snapshotOfVotersUpToNow;
	}

	@Override @View
	public final BigInteger getTotalVotesCastable() {
		return totalVotesCastable;
	}

	@Override @View
	public final BigInteger getVotesCastUpToNow() {
		return votesCastUpToNow;
	}

	@Override @FromContract
	public final void vote() {
		vote(eligibleVoters.get(caller()));
	}
	
	@SuppressWarnings("unchecked")
	@Override @FromContract
	public final void vote(BigInteger votes) {
		Contract caller = caller();
		checkIfCanVote(caller, votes);
		// this unsafe cast will always succeed, since checkIfCanVote has verified that the caller is an eligible voter
		votersUpToNow.put((Voter) caller, votes);
		votesCastUpToNow = votesCastUpToNow.add(votes);
		snapshotOfVotersUpToNow = votersUpToNow.snapshot();
	}

	@Override
	public void close() {
		require(!isClosed, "the poll is already closed");
		require(isOver(), "the poll is not over");
		if (goalReached())
			action.run();

		isClosed = true;
	}

	@Override
	public boolean isOver() {
		return goalReached() || votersUpToNow.size() == eligibleVoters.size();
	}

	protected void checkIfCanVote(Contract voter, BigInteger votes) {
		BigInteger max = eligibleVoters.get(voter);
		require(max != null, "you are not a shareholder");
		require(!votersUpToNow.containsKey(voter), "you have already voted");
		require(votes != null && ZERO.compareTo(votes) <= 0 && votes.compareTo(max) <= 0, () -> "you are only allowed to cast between 0 and " + max + "votes, inclusive");
	}

	/**
	 * Determines if the goal has been reached. By default, this means that
	 * at least 50%+1 of the eligible votes have been cast for this poll. Subclasses may redefine.
	 * 
	 * @return true if and only if the goal has been reached
	 */
	protected boolean goalReached() {
		return totalVotesCastable.compareTo(votesCastUpToNow.multiply(TWO)) < 0;
	}
}