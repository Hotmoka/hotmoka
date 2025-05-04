/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.examples.specialaccount;

import java.math.BigInteger;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

/**
 * Just a subclass of externally-owned account. It is used in the
 * Distributor test to simulate the addition of a distribution account
 * that lives in its own classpath, unrelated to that of the distributor itself.
 */
public class SpecialAccount extends ExternallyOwnedAccount {

	public SpecialAccount(String publicKey) {
		super(publicKey);
	}

	@Payable @FromContract
	public SpecialAccount(int initialAmount, String publicKey) {
		super(initialAmount, publicKey);
	}

	@Payable @FromContract
	public SpecialAccount(long initialAmount, String publicKey) {
		super(initialAmount, publicKey);
	}

	@Payable @FromContract
	public SpecialAccount(BigInteger initialAmount, String publicKey) {
		super(initialAmount, publicKey);
	}
}