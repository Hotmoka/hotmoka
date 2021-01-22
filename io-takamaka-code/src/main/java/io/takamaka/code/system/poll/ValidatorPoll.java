package io.takamaka.code.system.poll;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.system.GenericValidators;
import io.takamaka.code.system.Manifest;

public abstract class ValidatorPoll extends PollWithTimeWindow{
	
	/**
	 * The manifest of the node that instantiated @ValidatorPoll.
	 */
	protected final Manifest manifest;
	
	@FromContract(GenericValidators.class)
	public ValidatorPoll(Manifest manifest) {
		super();
		this.manifest = manifest;
	}
	
	@FromContract(GenericValidators.class)
	public ValidatorPoll(BigInteger startTime, BigInteger durationTime, Manifest manifest) {
		super(startTime, durationTime);
		this.manifest = manifest;
	}

	/**
	 *  The goal is reached when counter is greater than 50% of total
	 */
	@Override
	protected boolean isGoalReached() {
		return counter.compareTo(total.divide(BigInteger.TWO)) > 0;
	}

	@FromContract(GenericValidators.class)
	public void closePoll() {
		super.closePoll();
	}
}
