package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;

public class PollWithTimeWindow extends SimplePoll {
	
	/** 
	 * The time when the @Poll instance has been created.
	 */
	private final long creationTime;
	
	/** 
	 * The time that must pass from the creation of the @Poll instance before the start of voting.
	 */
	private final long startTime;
	
	/** 
	 * The duration of the voting after it has started.
	 */
	private final long durationTime;
	
	/** 
	 * The start of time window
	 */
	private final long startWindow;
	
	/** 
	 * The end of time window
	 */
	private final long endWindow;
	
	public PollWithTimeWindow(SharedEntity<?> shareholders, Action action) {
		this(shareholders, action, 0, Math.subtractExact(Long.MAX_VALUE, now()));
	}
	
	public PollWithTimeWindow(SharedEntity<?> shareholders, Action action, long startTime, long durationTime) {
		super(shareholders, action);

		require(startTime >= 0 && durationTime >= 0, "the time parameters cannot be negative");
		
		this.creationTime = now();
		this.startTime = startTime;
		this.durationTime = durationTime;
		this.startWindow = Math.addExact(creationTime,startTime);
		this.endWindow = Math.addExact(startWindow, durationTime);

	}

	@Override
	protected void checkIfCanVote(Contract voter, BigInteger weight) {
		super.checkIfCanVote(voter, weight);
		require(isValidTimeWindow(), "we are currently outside the time window for voting");
	}

	private boolean isValidTimeWindow() {
		return startWindow <= now() && now() < endWindow;
	}

	@Override
	public boolean isOver() {
		return super.isOver() || (now() >= startWindow && !isValidTimeWindow());
	}	
}