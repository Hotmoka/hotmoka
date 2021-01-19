package io.takamaka.code.system;

import java.math.BigInteger;

import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.RequirementViolationException;
import io.takamaka.code.lang.View;
import io.takamaka.code.dao.SimpleSharedEntity;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.StorageMap;
import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;

public abstract class Poll extends Contract{
	/** 
	 * Snapshot of shares at @Poll instantiation.
	 */
	protected final StorageMapView<PayableContract, BigInteger> shares;
	protected final BigInteger total;
	protected final StorageMap<Contract, BigInteger> voted;
	
	/** 
	 * 
	 * The time when the @Poll instance is created
	 */
	protected final long creationTime;
	/** 
	 * 
	 * The time that must pass from the creation of @Poll instance before the start of voting
	 */
	protected final long startTime;
	
	/** 
	 * 
	 * The duration of the voting after it has started
	 */
	protected final long durationTime;
	
	
	@FromContract(SimpleSharedEntity.class)
	public Poll() {
		shares = ((SimpleSharedEntity) caller()).getShares();
		total = BigInteger.ZERO;
		shares.forEach(e -> total.add(e.getValue()));
		voted = new StorageTreeMap<>();
		creationTime = now();
		startTime= 0;
		durationTime = Math.subtractExact(Long.MAX_VALUE, creationTime);

	}
	
	@FromContract(SimpleSharedEntity.class)
	public Poll(long startTime, long durationTime) {
		shares = ((SimpleSharedEntity) caller()).getShares();
		total = BigInteger.ZERO;
		shares.forEach(e -> total.add(e.getValue()));
		voted = new StorageTreeMap<>();
		creationTime = now();
		require(startTime >= 0 && durationTime >= 0, () -> "invalid time parameters");
		this.startTime = startTime;
		this.durationTime = durationTime;
	}
	
	/** 
	 * Vote with maximum share at the time of @Poll instantiation.
	 */
	@FromContract(PayableContract.class)
	public void vote() {
		PayableContract pc = (PayableContract) caller();
		vote(pc, shares.get(pc));
	}
	
	/** 
	 *  Vote with a share between zero and the maximum share at the time of @Poll instantiation.
	 */
	@FromContract(PayableContract.class)
	public void vote(BigInteger share) {
		PayableContract pc = (PayableContract) caller();
		require(share != null && share.compareTo(BigInteger.ZERO) > 1 && share.compareTo(shares.get(pc)) < 1, () -> "invalid amount of power: " + share);
		vote(pc, share);
	}
	
	private void vote(PayableContract pc, BigInteger share) {
		require(isValidTimeWindow(), () -> "invalid time window" );
		require(pc != null && share != null, () -> "invalid parameters");
		require(shares.containsKey(pc), () -> "you must be in the shares map to vote");
		require(!voted.containsKey(pc), () -> "you already have voted");
		voted.put(pc, share);
	}
	
	
	protected void incrementCounter(BigInteger share) {
		// TODO
	}
	
	
	protected void closePoll() {
		if( isGoalReached() ){
			action();
		}
	}
	
	protected boolean isGoalReached() {
		// TODO
		return false;
	}
	
	protected abstract void action();
	
	@View
	public BigInteger voted() {
		// TODO
		return BigInteger.ONE;
	}
	
	@View
	public BigInteger getTotal() {
		return total;
	}
	
	@View
	public boolean hasVoted(Contract pc) {
		return voted.containsKey(pc);
	}

	protected boolean isValidTimeWindow() {
		long now = now();
		long startWindow = Math.addExact(creationTime, startTime);
		long endWindow = Math.addExact(startWindow, durationTime);
		
		return startWindow <= now && now <= endWindow;
	}
	
}
