package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.isSystemCall;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

/**
 * The manager of the versions of the modules of the node.
 * This object keeps track of the current versions and modifies
 * them when needed.
 */
@Exported
public class Versions extends Storage {

	/**
	 * The validators of the node, that have the right to modify
	 * the versions of the modules of the node, through some form of poll.
	 */
	@SuppressWarnings("unused")
	private final Validators validators;

	/**
	 * The current version of the verification module.
	 */
	private int verificationVersion;

	/**
	 * Builds an object that keeps track of the versions of the modules of the node.
	 * 
	 * @param validators the validators that have the right to modify the versions
	 *                   of the modules of this node, through some form of poll
	 */
	Versions(Validators validators) {
		this.validators = validators;
	}

	/**
	 * Yields the current version of the verification module.
	 * 
	 * @return the current version of the verification module
	 */
	public final int getVerificationVersion() {
		return verificationVersion;
	}

	// TODO: make private at the end and increase it through a poll among the validators
	public final void increaseVerificationVersion() {
		require(isSystemCall(), "the verification version can only be increased with a system request");

		verificationVersion++;
	}
}