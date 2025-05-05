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

package io.hotmoka.moka.internal.json;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.moka.api.accounts.AccountsShowOutput;
import io.hotmoka.moka.internal.accounts.Show;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka accounts show} command.
 */
public abstract class AccountsShowOutputJson implements JsonRepresentation<AccountsShowOutput> {
	private final BigInteger balance;
	private final String signature;
	private final String publicKeyBase58;
	private final String publicKeyBase64;

	protected AccountsShowOutputJson(AccountsShowOutput output) {
		this.balance = output.getBalance();
		this.signature = output.getSignature().getName();
		this.publicKeyBase58 = output.getPublicKeyBase58();
		this.publicKeyBase64 = output.getPublicKeyBase64();
	}

	public BigInteger getBalance() {
		return balance;
	}

	public String getSignature() {
		return signature;
	}

	public String getPublicKeyBase58() {
		return publicKeyBase58;
	}

	public String getPublicKeyBase64() {
		return publicKeyBase64;
	}

	@Override
	public AccountsShowOutput unmap() throws InconsistentJsonException, NoSuchAlgorithmException {
		return new Show.Output(this);
	}
}