package io.takamaka.code.system.poll;

import java.math.BigInteger;

public interface Votable {

	/**
	 * Perform the vote
	 * @param value quantifies the value of vote
	 */
	public void vote(BigInteger value);
	
	/**
	 * Yield the amount of values reached from voters
	 */
	public BigInteger voted();
	
	/**
	 * Yield the number of voters
	 */
	public BigInteger votersCounter();
	
	/**
	 * Check if the vote is over
	 */
	public boolean isVoteOver();
	
}
