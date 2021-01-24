package io.takamaka.code.system.poll;

import static io.takamaka.code.lang.Takamaka.require;
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
public abstract class Poll extends Storage {

	/** 
	 * Snapshot of shares at @Poll instantiation.
	 */
	private final StorageMapView<PayableContract, BigInteger> shares;
	private final StorageSet<PayableContract> votersUpToNow = new StorageTreeSet<>();
	private BigInteger votesInFavorUpToNow = ZERO;
	private boolean closed;
	protected final BigInteger total;

	@FromContract
	public Poll() {
		require(caller() instanceof SharedEntity<?>, "only a shared entity can start a poll");
		shares = ((SharedEntity<?>) caller()).getShares();
		total = shares.stream().map(Entry::getValue).reduce(ZERO, BigInteger::add);
	}
	
	@FromContract
	protected Poll(SharedEntity<?> sse) {
		require(caller() instanceof SharedEntity<?>, "only a shared entity can start a poll");
		shares = sse.getShares();
		total = shares.stream().map(Entry::getValue).reduce(ZERO, BigInteger::add);
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
		require(!closed, "poll already closed");
		require(isOver(), "poll is not over");
		if (isGoalReached())
			action();

		closed = true;
	}
	
	public boolean isOver() {
		return isGoalReached() || numbersOfVotersUpToNow() == shares.size();
	}
	
	protected abstract boolean isGoalReached();
	
	protected abstract void action();
	
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