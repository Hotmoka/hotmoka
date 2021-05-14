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

package io.hotmoka.examples.constructoronthis;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;

public class Bridge2 extends Contract {
	private Sub2 sub;
	private BigInteger initialBalance;

	public @FromContract @Payable void foo(int amount) {
		initialBalance = balance();
		sub = new Sub2(amount);
	}

	public @View BigInteger getBalance() {
		return balance();
	}

	public @View BigInteger getInitialBalance() {
		return initialBalance;
	}

	public @View BigInteger getInitialBalanceOfSub() {
		return sub.getInitialBalance();
	}

	public @View BigInteger getBalanceOfSub() {
		return sub.getBalance();
	}
}