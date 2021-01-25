package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;

public class PollWithTimeWindow extends SimplePoll {
	
	/** 
	 * The time when the @Poll instance has been created.
	 */
	private final BigInteger creationTime;
	
	/** 
	 * The time that must pass from the creation of the @Poll instance before the start of voting.
	 */
	private final BigInteger startTime;
	
	/** 
	 * The duration of the voting after it has started.
	 */
	private final BigInteger durationTime;
	
	/**
	 * Boolean flag to know if the time window is expired
	 */
	private boolean timeWindowExpired;
	
	public PollWithTimeWindow(SharedEntity<?> shareholders, Action action) {
		this(shareholders, action, ZERO, BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.valueOf(now())));
	}
	
	public PollWithTimeWindow(SharedEntity<?> shareholders, Action action, BigInteger startTime, BigInteger durationTime) {
		super(shareholders, action);

		require(startTime.signum() >= 0 && durationTime.signum() >= 0, "the time parameters cannot be negative");
		this.creationTime = BigInteger.valueOf(now());
		this.startTime = startTime;
		this.durationTime = durationTime;
	}

	@Override
	protected void checkIfCanVote(Contract voter, BigInteger weight) {
		super.checkIfCanVote(voter, weight);
		require(isValidTimeWindow(), "we are currently outside the time window for voting");
	}

	private boolean isValidTimeWindow() {
		if (timeWindowExpired)
			return false;

		BigInteger now = BigInteger.valueOf(now());
		BigInteger startWindow = creationTime.add(startTime);
		BigInteger endWindow = startWindow.add(durationTime);

		if (startWindow.compareTo(now) <= 0 && now.compareTo(endWindow) < 0)
			return true;
		else if (now.compareTo(endWindow) >= 0)
			timeWindowExpired = true; // necessary because if now() performs an overflow in the future, 
									  // the contract could return available. Instead with the timeWindowExpired 
									  // set to true, it is avoided.
		return false;
	}

	@Override
	public boolean isOver() {
		return super.isOver() || !isValidTimeWindow();
	}	
}