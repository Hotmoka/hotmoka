package io.takamaka.code.system.poll;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.system.GenericValidators;
import io.takamaka.code.system.Manifest;

public class IncreaseVersionValidatorsPoll extends ValidatorsPoll {
	
	@FromContract(GenericValidators.class)
	public IncreaseVersionValidatorsPoll(Manifest manifest) {
		super(manifest);
	}
	
	@FromContract(GenericValidators.class)
	public IncreaseVersionValidatorsPoll(BigInteger startTime, BigInteger durationTime, Manifest manifest) {
		super(startTime, durationTime, manifest);
	}

	@Override
	protected void action() {
		manifest.versions.increaseVerificationVersion();
	}
}