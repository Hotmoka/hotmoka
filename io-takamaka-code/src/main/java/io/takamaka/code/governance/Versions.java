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
	private int verificationVersion;

	/**
	 * Builds an object that keeps track of the versions of the modules of the node.
	 * 
	 * @param manifest the manifest of the node
	 * @param verificationVersion the version of the verification module to use
	 */
	Versions(Manifest<V> manifest, int verificationVersion) {
		this.manifest = manifest;
		this.verificationVersion = verificationVersion;
	}

	/**
	 * Yields the current version of the verification module.
	 * 
	 * @return the current version of the verification module
	 */
	public final @View int getVerificationVersion() {
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
		return manifest.validators.newPoll(amount, increaseVerificationVersion);
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
		return manifest.validators.newPoll(amount, increaseVerificationVersion, start, duration);
	}

	private void generateVerificationVersionUpdateEvent() {
		event(new VerificationVersionUpdate(verificationVersion));
	}

	/**
	 * An action that sets the verification version of the network to the
	 * verification version at the time of creation of the action, plus one.
	 */
	private Action increaseVerificationVersion = new Action() {

		private final int newVerificationVersion = verificationVersion + 1;

		@Override
		protected String getDescription() {
			return "sets the verification version of the network to " + newVerificationVersion;
		}

		@Override
		protected void run() {
			verificationVersion = newVerificationVersion;
			generateVerificationVersionUpdateEvent();
		}
	};
}