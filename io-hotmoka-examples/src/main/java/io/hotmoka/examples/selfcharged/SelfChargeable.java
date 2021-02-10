package io.hotmoka.examples.selfcharged;

import static io.takamaka.code.lang.Takamaka.event;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.SelfCharged;
import io.takamaka.code.selfcharged.BlackList;
import io.takamaka.code.selfcharged.WhiteList;

public class SelfChargeable extends Contract {
	public int i;

	// we allow to charge the contract at construction time
	public @Payable @FromContract SelfChargeable(int amount) {}

	// this requires some balance if an account wants to call it
	public void foo() {
		i++;
	}

	// this can be called also by an account with zero balance
	public @SelfCharged void goo() {
		i++;
	}

	/**
	 * An example of generation of events for adding/removing from the white-list
	 * of accounts that can call {@code @@SelfCharged} methods, if any such white-list exist.
	 */
	public void whiteOrBlack(int i, String account) {
		if (i % 2 == 0)
			event(new WhiteList(account));
		else
			event(new BlackList(account));
	}
}