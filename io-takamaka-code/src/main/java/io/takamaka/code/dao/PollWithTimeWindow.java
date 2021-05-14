/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.View;

/**
 * A poll whose goal must be reached during a time window.
 *
 * @param <Voter> the type of the voters
 */
public class PollWithTimeWindow<Voter extends Contract> extends SimplePoll<Voter> {
	
	/** 
	 * The start of time window.
	 */
	private final long startWindow;
	
	/** 
	 * The end of time window.
	 */
	private final long endWindow;

	/**
	 * Creates a poll whose votes can only be cast inside a given time window.
	 * 
	 * @param voters the eligible voters, with their associated power
	 * @param action the action to run if the goal is reached
	 * @param startTime the starting time of the time window, in milliseconds from now
	 * @param durationTime the duration of the time window, in milliseconds
	 */
	public PollWithTimeWindow(SharedEntityView<Voter> voters, Action action, long startTime, long durationTime) {
		super(voters, action);

		require(startTime >= 0 && durationTime >= 0, "the time parameters cannot be negative");
		
		this.startWindow = Math.addExact(now(), startTime);
		this.endWindow = Math.addExact(startWindow, durationTime);

	}

	@Override
	protected void checkIfCanVote(Contract voter, BigInteger weight) {
		super.checkIfCanVote(voter, weight);
		require(isValidTimeWindow(), "we are currently outside the time window for voting");
	}

	private boolean isValidTimeWindow() {
		long now = now();
		return startWindow <= now && now < endWindow;
	}

	@Override
	@View
	public boolean isOver() {
		return super.isOver() || now() >= endWindow;
	}	
}