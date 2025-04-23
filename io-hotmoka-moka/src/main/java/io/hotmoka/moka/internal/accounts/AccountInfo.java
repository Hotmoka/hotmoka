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

package io.hotmoka.moka.internal.accounts;

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * Information about an account.
 */
@Immutable
public class AccountInfo {
	public final BigInteger balance;
	public final String signature;
	public final String publicKeyBase58;
	public final String publicKeyBase64;

	/**
	 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
	 */
	public final static int MAX_PRINTED_KEY = 200;

	public AccountInfo(BigInteger balance, SignatureAlgorithm signature, String publicKeyBase64) throws CommandException {
		this.balance = balance;
		this.signature = signature.getName();
		this.publicKeyBase64 = publicKeyBase64;

		try {
			this.publicKeyBase58 = Base58.toBase58String(Base64.fromBase64String(publicKeyBase64));
		}
		catch (Base64ConversionException e) {
			throw new CommandException("The public key in the account is not in Base64 format", e);
		}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AccountInfo ai
			&& ai.signature.equals(signature)
			&& ai.publicKeyBase58.equals(publicKeyBase58)
			&& ai.publicKeyBase64.equals(publicKeyBase64);
	}

	@Override
	public String toString() {
		String result = "* balance: " + balance + "\n";

		if (publicKeyBase58.length() > MAX_PRINTED_KEY)
			result += "* public key: " + publicKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)";
		else
			result += "* public key: " + publicKeyBase58 + " (" + signature + ", base58)";

		if (publicKeyBase64.length() > MAX_PRINTED_KEY)
			result += "\n* public key: " + publicKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)";
		else
			result += "\n* public key: " + publicKeyBase64 + " (" + signature + ", base64)";

		return result;
	}
}