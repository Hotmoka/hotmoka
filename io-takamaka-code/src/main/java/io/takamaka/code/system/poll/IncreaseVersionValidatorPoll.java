package io.takamaka.code.system.poll;

import java.math.BigInteger;

import io.takamaka.code.system.Manifest;

public class IncreaseVersionValidatorPoll extends ValidatorPoll {
	
	public IncreaseVersionValidatorPoll(Manifest manifest) {
		super(manifest);
	}
	
	public IncreaseVersionValidatorPoll(BigInteger startTime, BigInteger durationTime, Manifest manifest) {
		super(startTime, durationTime, manifest);
	}

	@Override
	protected void action() {
		manifest.versions.increaseVerificationVersion();
	}

}
