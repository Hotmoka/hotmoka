package io.takamaka.code.system.poll;

import java.math.BigInteger;

import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.dao.SimpleSharedEntity;
import io.takamaka.code.lang.FromContract;
import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;

public abstract class PollWithTimeWindow extends Poll {
	
	/** 
	 * 
	 * The time when the @Poll instance is created
	 */
	protected final BigInteger creationTime;
	
	/** 
	 * 
	 * The time that must pass from the creation of @Poll instance before the start of voting
	 */
	protected final BigInteger startTime;
	
	/** 
	 * 
	 * The duration of the voting after it has started
	 */
	protected final BigInteger durationTime;
	
	/**
	 * Boolean flag to know if the time window is expired
	 */
	private boolean timeWindowExpired;
	
	@FromContract(SimpleSharedEntity.class)
	public PollWithTimeWindow() {
		super();
		creationTime = BigInteger.valueOf(now());
		startTime = BigInteger.ZERO;
		durationTime = BigInteger.valueOf(Long.MAX_VALUE).subtract(creationTime);
	}
	
	@FromContract(SimpleSharedEntity.class)
	public PollWithTimeWindow(BigInteger startTime, BigInteger durationTime) {
		super();
		creationTime = BigInteger.valueOf(now());
		require(startTime.compareTo(BigInteger.ZERO) >= 0 && durationTime.compareTo(BigInteger.ZERO) >= 0, () -> "invalid time parameters");
		this.startTime = startTime;
		this.durationTime = durationTime;
	}
	
	@Override
	protected void vote(PayableContract pc, BigInteger share) {
		require(isValidTimeWindow() && !timeWindowExpired, () -> "invalid time window" );
		super.vote(pc, share);
	}

	private boolean isValidTimeWindow() {
		BigInteger now = BigInteger.valueOf(now());
		BigInteger startWindow = creationTime.add(startTime);
		BigInteger endWindow = startWindow.add(durationTime);
		
		if(startWindow.compareTo(now) < 0 && endWindow.compareTo(now) > 1)
			return true;
		else if(endWindow.compareTo(now) > 1)
			timeWindowExpired = true; // necessary because if now() performs an overflow in the future, 
									  // the contract could return available. Instead with the timeWindowExpired 
									  // set to true, it is avoided.
		return false;
		
	}
	
	@Override
	public boolean isVoteOver() {
		return super.isVoteOver() || (!isValidTimeWindow() && timeWindowExpired);
	}
	
}
