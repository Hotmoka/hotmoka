package io.takamaka.code.system.poll;

import io.takamaka.code.system.Manifest;

public class IncreaseVersionValidatorPoll extends ValidatorPoll {
	
	public IncreaseVersionValidatorPoll(Manifest manifest) {
		super(manifest);
	}

	@Override
	protected void action() {
		manifest.versions.increaseVerificationVersion();
	}

}
