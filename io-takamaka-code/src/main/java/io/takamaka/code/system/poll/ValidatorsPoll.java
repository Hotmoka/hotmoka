package io.takamaka.code.system.poll;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.TWO;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.system.Manifest;
import io.takamaka.code.system.Validators;

public abstract class ValidatorsPoll extends PollWithTimeWindow {
	
	/**
	 * The manifest of the node that instantiated @ValidatorPoll.
	 */
	protected final Manifest manifest;
	
	@FromContract
	public ValidatorsPoll(Manifest manifest) {
		require(caller() instanceof Validators, "only a set of validators can start a validators poll");
		this.manifest = manifest;
	}
	
	@FromContract
	public ValidatorsPoll(BigInteger startTime, BigInteger durationTime, Manifest manifest) {
		super(startTime, durationTime);
		require(caller() instanceof Validators, "only a set of validators can start a validators poll");
		this.manifest = manifest;
	}

	/**
	 *  The goal is reached when counter is greater than 50% of total
	 */
	@Override
	protected boolean isGoalReached() {
		return getVotesInFavorUpToNow().multiply(TWO).compareTo(total) > 0;
	}
}