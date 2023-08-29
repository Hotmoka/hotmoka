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

package io.takamaka.code.governance;

import static io.takamaka.code.lang.Takamaka.event;

import java.math.BigInteger;

import io.takamaka.code.dao.SimplePoll;
import io.takamaka.code.dao.SimplePoll.Action;
import io.takamaka.code.dao.PollWithTimeWindow;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;

/**
 * The manager of the versions of the modules of the node.
 * This object keeps track of the current versions and modifies
 * them when needed.
 * 
 * @param <V> the type of the validators of the manifest having this versions object
 */
public class Versions<V extends Validator> extends Contract {

	/**
	 * The manifest of the node.
	 */
	private final Manifest<V> manifest;

	/**
	 * The current version of the verification module.
	 */
	private long verificationVersion;

	/**
	 * Builds an object that keeps track of the versions of the modules of the node.
	 * 
	 * @param manifest the manifest of the node
	 * @param verificationVersion the version of the verification module to use
	 */
	Versions(Manifest<V> manifest, long verificationVersion) {
		this.manifest = manifest;
		this.verificationVersion = verificationVersion;
	}

	/**
	 * Yields the current version of the verification module.
	 * 
	 * @return the current version of the verification module
	 */
	public final @View long getVerificationVersion() {
		return verificationVersion;
	}

	/**
	 * Starts a new poll among the validators, with the goal to increase the version of the verification module.
	 * 
	 * @param amount the amount of coins payed to start the poll
	 * @return the new poll
	 */
	@Payable @FromContract
	public final SimplePoll<V> newPollToIncreaseVerificationVersion(BigInteger amount) {
		return manifest.validators.newPoll(amount, new IncreaseVerificationVersion());
	}

	/**
	 * Starts a new poll among the validators, with the given time window,
	 * with the goal to increase the version of the verification module.
	 * 
	 * @param amount the amount of coins payed to start the poll
	 * @param start the starting moment of the poll
	 * @param duration the duration of the poll
	 * @return the new poll
	 */
	@Payable @FromContract
	public final PollWithTimeWindow<V> newPollToIncreaseVerificationVersion(BigInteger amount, long start, long duration) {
		return manifest.validators.newPoll(amount, new IncreaseVerificationVersion(), start, duration);
	}

	private void generateVerificationVersionUpdateEvent() {
		event(new VerificationVersionUpdate(verificationVersion));
	}

	/**
	 * An action that sets the verification version of the network to the
	 * verification version at the time of creation of the action, plus one.
	 */
	private class IncreaseVerificationVersion extends Action {

		private final long newVerificationVersion = verificationVersion + 1;

		@Override
		public String getDescription() {
			return "sets the verification version of the network to " + newVerificationVersion;
		}

		@Override
		protected void run() {
			verificationVersion = newVerificationVersion;
			generateVerificationVersionUpdateEvent();
		}
	}
}