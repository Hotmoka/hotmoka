package io.takamaka.code.system.poll;

import java.math.BigInteger;

public interface Votable {

	/**
	 * Perform the vote
	 * @param weight quantifies the value of vote
	 */
	public void vote(BigInteger weight);
	
	/**
	 * Yield the number of votes
	 */
	public BigInteger votesCount();
	
	/**
	 * Check if the vote is over
	 */
	public boolean isVoteOver();
	
}
