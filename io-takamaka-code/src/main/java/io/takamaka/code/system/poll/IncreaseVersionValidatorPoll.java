package io.takamaka.code.system.poll;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.system.GenericValidators;
import io.takamaka.code.system.Manifest;

public class IncreaseVersionValidatorPoll extends ValidatorPoll {
	
	@FromContract(GenericValidators.class)
	public IncreaseVersionValidatorPoll(Manifest manifest) {
		super(manifest);
	}
	
	@FromContract(GenericValidators.class)
	public IncreaseVersionValidatorPoll(BigInteger startTime, BigInteger durationTime, Manifest manifest) {
		super(startTime, durationTime, manifest);
	}

	@Override
	protected void action() {
		manifest.versions.increaseVerificationVersion();
	}

}
