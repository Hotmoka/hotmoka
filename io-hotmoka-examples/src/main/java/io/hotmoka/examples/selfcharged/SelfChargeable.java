/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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