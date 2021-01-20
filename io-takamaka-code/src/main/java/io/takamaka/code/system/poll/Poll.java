package io.takamaka.code.system.poll;

import java.math.BigInteger;

import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.dao.SimpleSharedEntity;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.StorageMap;
import static io.takamaka.code.lang.Takamaka.require;

public abstract class Poll extends Contract implements Votable {
	/** 
	 * Snapshot of shares at @Poll instantiation.
	 */
	protected final StorageMapView<PayableContract, BigInteger> shares;
	protected final BigInteger total;
	protected final StorageMap<Contract, BigInteger> voted;
	
	@FromContract(SimpleSharedEntity.class)
	public Poll() {
		shares = ((SimpleSharedEntity) caller()).getShares();
		total = BigInteger.ZERO;
		shares.forEach(e -> total.add(e.getValue()));
		voted = new StorageTreeMap<>();
	}
	
	@FromContract(SimpleSharedEntity.class)
	protected Poll(SimpleSharedEntity sse) {
		shares = sse.getShares();
		total = BigInteger.ZERO;
		shares.forEach(e -> total.add(e.getValue()));
		voted = new StorageTreeMap<>();
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
	@Override
	@FromContract(PayableContract.class)
	public void vote(BigInteger share) {
		PayableContract pc = (PayableContract) caller();
		require(share != null && share.compareTo(BigInteger.ZERO) > 1 && share.compareTo(shares.get(pc)) < 1, () -> "invalid amount of power: " + share);
		vote(pc, share);
	}
	
	protected void vote(PayableContract pc, BigInteger share) {
		require(pc != null && share != null, () -> "invalid parameters");
		require(shares.containsKey(pc), () -> "you must be in the shares map to vote");
		require(!voted.containsKey(pc), () -> "you already have voted");
		voted.put(pc, share);
	}
	
	
	protected void incrementCounter(BigInteger share) {
		// TODO
	}
	
	@Override
	public BigInteger votersCounter() {
		//TODO
		return BigInteger.ZERO;
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
	@Override
	public BigInteger voted() {
		// TODO
		return BigInteger.ONE;
	}
	
	@View
	public BigInteger getTotal() {
		return total;
	}
	
	@View
	public boolean hasVoted(Contract c) {
		return voted.containsKey(c);
	}
	
}
