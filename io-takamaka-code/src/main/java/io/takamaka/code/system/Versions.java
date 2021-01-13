package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.event;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.View;

/**
 * The manager of the versions of the modules of the node.
 * This object keeps track of the current versions and modifies
 * them when needed.
 */
@Exported
public class Versions extends Contract {

	/**
	 * The manifest of the node.
	 */
	@SuppressWarnings("unused")
	private final Manifest manifest;

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
	Versions(Manifest manifest, int verificationVersion) {
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

	// TODO: make private at the end and increase it through a poll among the validators
	final void increaseVerificationVersion() {
		verificationVersion++;
		event(new ConsensusUpdate("the version of the verification module has been set to " + verificationVersion));
	}
}