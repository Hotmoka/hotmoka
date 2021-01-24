package io.takamaka.code.system.poll;

import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.PayableContract;

public class PollWithTimeWindow extends Poll {
	
	/** 
	 * The time when the @Poll instance is created
	 */
	private final BigInteger creationTime;
	
	/** 
	 * The time that must pass from the creation of @Poll instance before the start of voting
	 */
	private final BigInteger startTime;
	
	/** 
	 * The duration of the voting after it has started
	 */
	private final BigInteger durationTime;
	
	/**
	 * Boolean flag to know if the time window is expired
	 */
	private boolean timeWindowExpired;
	
	@FromContract
	public PollWithTimeWindow(SharedEntity<?> sharedEntity, Action action) {
		this(sharedEntity, action, ZERO, BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.valueOf(now())));
	}
	
	@FromContract
	public PollWithTimeWindow(SharedEntity<?> sharedEntity, Action action, BigInteger startTime, BigInteger durationTime) {
		super(sharedEntity, action);

		require(startTime.signum() >= 0 && durationTime.signum() >= 0, "invalid time parameters");
		this.creationTime = BigInteger.valueOf(now());
		this.startTime = startTime;
		this.durationTime = durationTime;
	}

	@Override
	protected void checkIfCanVote(PayableContract voter, BigInteger weight) {
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