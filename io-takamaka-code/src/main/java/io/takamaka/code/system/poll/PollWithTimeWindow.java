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
	public PollWithTimeWindow() {
		super();
		creationTime = now();
		startTime = 0;
		durationTime = Math.subtractExact(Long.MAX_VALUE, creationTime);
	}
	
	@FromContract(SimpleSharedEntity.class)
	public PollWithTimeWindow(long startTime, long durationTime) {
		super();
		creationTime = now();
		require(startTime >= 0 && durationTime >= 0, () -> "invalid time parameters");
		this.startTime = startTime;
		this.durationTime = durationTime;
	}
	
	@Override
	protected void vote(PayableContract pc, BigInteger share) {
		require(isValidTimeWindow(), () -> "invalid time window" );
		super.vote(pc, share);
	}

	private boolean isValidTimeWindow() {
		long now = now();
		long startWindow = Math.addExact(creationTime, startTime);
		long endWindow = Math.addExact(startWindow, durationTime);
		
		return startWindow <= now && now <= endWindow;
	}
	
}
