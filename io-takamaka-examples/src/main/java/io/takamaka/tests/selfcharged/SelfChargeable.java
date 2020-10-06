package io.takamaka.tests.selfcharged;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.SelfCharged;

public class SelfChargeable extends Contract {
	public int i;

	// we allow to charge the contract at construction time
	public @Payable @Entry SelfChargeable(int amount) {}

	// this requires some balance if an account wants to call it
	public void foo() {
		i++;
	}

	// this can be called also by an account with zero balance
	public @SelfCharged void goo() {
		i++;
	}
}