package io.takamaka.code.dao;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMapView;

/**
 * A poll among a set of voters (contracts). Each voter can vote with a number of votes
 * between one and its power.
 * 
 * @param <Voter> the type of the voters for this poll
 */
public interface Poll<Voter extends Contract> {

	/**
	 * Yields the description of this poll.
	 * 
	 * @return the description
	 */
	@View
	String getDescription();

	/**
	 * Yields the voters that are allowed to vote for this poll, with the
	 * maximal number of votes that each of them can cast (its power).
	 * 
	 * @return the voters allowed to vote
	 */
	@View
	StorageMapView<Voter, BigInteger> getEligibleVoters();

	/**
	 * Yields a snapshot of the voters that have already voted for this poll, with the
	 * number of votes that each of them has cast.
	 * 
	 * @return the voters that have already voted
	 */
	@View
	StorageMapView<Voter, BigInteger> getVotersUpToNow();

	/**
	 * Yields the maximal number of votes that can be cast for this poll.
	 * This is the sum of the votes of all eligible voters.
	 * 
	 * @return the votes maximal number of votes that can be cast
	 */
	@View
	BigInteger getTotalVotesCastable();

	/**
	 * Yields the votes cast up to now for this poll. This is the sum of the votes
	 * of all voters up to now.
	 * 
	 * @return the votes cast up to now. This is between zero and {@link #getTotalVotesCastable()}
	 */
	@View
	BigInteger getVotesCastUpToNow();

	/** 
	 * An eligible voter calls this method to vote in favor of this poll, with all its power.
	 * An eligible voter cannot vote twice.
	 */
	@FromContract
	void vote();
	
	/** 
	 * An eligible voter calls this method to vote in favor of this poll, with a subset of its power.
	 * An eligible voter cannot vote twice.
	 *
	 * @param votes the votes in favor of this poll, between 1 and the power of the calling voter
	 */
	@FromContract
	void vote(BigInteger votes);

	/**
	 * Checks if the poll is over, that is, its goal has been reached or it is not possible
	 * anymore to cast new votes.
	 * 
	 * @return true if and only if the poll is over
	 */
	@View
	boolean isOver();

	/**
	 * Closes the poll, if it is over. A poll cannot be closed twice.
	 */
	void close();
}