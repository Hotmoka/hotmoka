package io.takamaka.code.system;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

/**
 * An event issued when the version of the verification module has been updated.
 */
public class VerificationVersionUpdate extends ConsensusUpdate {
	public final int newVerificationVersion;

	@FromContract VerificationVersionUpdate(int newVerificationVersion) {
		super("the version of the verification module has been set to " + newVerificationVersion);

		this.newVerificationVersion = newVerificationVersion;
	}

	public @View int getVerificationVersion() {
		return newVerificationVersion;
	}
}