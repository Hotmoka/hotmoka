package io.takamaka.code.system.poll;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageMapView.Entry;
import io.takamaka.code.util.StorageSet;
import io.takamaka.code.util.StorageTreeSet;

@Exported
public class Poll extends Storage {

	/**
	 * An action that is triggered if the goal of the poll has been reached.
	 */
	public static abstract class Action extends Storage implements Runnable {}

	/** 
	 * Snapshot of shares at @Poll instantiation.
	 */
	private final StorageMapView<PayableContract, BigInteger> shares;
	private final StorageSet<PayableContract> votersUpToNow = new StorageTreeSet<>();
	private final Action action;
	private BigInteger votesInFavorUpToNow = ZERO;
	private boolean isClosed;
	
	protected final BigInteger total;

	public Poll(SharedEntity<?> sharedEntity, Action action) {
		this.shares = sharedEntity.getShares();
		this.total = shares.stream().map(Entry::getValue).reduce(ZERO, BigInteger::add);
		this.action = action;
	}

	/** 
	 * Vote with all shares of the caller at the time of @Poll instantiation.
	 */
	@FromContract(PayableContract.class)
	public final void vote() {
		vote(shares.get(caller()));
	}
	
	/** 
	 *  Vote with a share between zero and the maximum share at the time of @Poll instantiation.
	 */
	@FromContract(PayableContract.class)
	public final void vote(BigInteger votes) {
		PayableContract voter = (PayableContract) caller();
		checkIfCanVote(voter, votes);
		votersUpToNow.add(voter);
		votesInFavorUpToNow.add(votes);
	}

	protected void checkIfCanVote(PayableContract voter, BigInteger votes) {
		BigInteger max = shares.get(voter);
		require(max != null, "you are not a shareholder");
		require(!hasVoted(voter), "you have already voted");
		require(votes != null && ZERO.compareTo(votes) < 0 && votes.compareTo(max) <= 0, () -> "you are only allowed to vote with a weight between 1 and " + max + "inclusive");
	}
	
	@View	
	public final int numbersOfVotersUpToNow() {
		return votersUpToNow.size();
	}
	
	public final void closePoll() {
		require(!isClosed, "poll already closed");
		require(isOver(), "poll is not over");
		if (isGoalReached())
			action.run();

		isClosed = true;
	}
	
	public boolean isOver() {
		return isGoalReached() || numbersOfVotersUpToNow() == shares.size();
	}

	/**
	 * Determines if the goal has been reached.
	 * By default, this means that at least 50%+1 of the vites are in favor.
	 * subclasses may redefine.
	 * 
	 * @return true if and only if the goal has been reached
	 */
	protected boolean isGoalReached() {
		return getVotesInFavorUpToNow().multiply(TWO).compareTo(total) > 0;
	}
	
	@View
	public final BigInteger getTotalVotesExpressible() {
		return total;
	}

	@View
	public final BigInteger getVotesInFavorUpToNow() {
		return votesInFavorUpToNow;
	}

	@View
	public final boolean hasVoted(PayableContract shareholder) {
		return votersUpToNow.contains(shareholder);
	}
}