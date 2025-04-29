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

package io.hotmoka.moka.api.accounts;

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * The output of the {@code moka accounts show} command.
 */
@Immutable
public interface AccountsShowOutput {

	/**
	 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
	 */
	final static int MAX_PRINTED_KEY = 200;

	/**
	 * Yields the balance of the account.
	 * 
	 * @return the balance of the account
	 */
	BigInteger getBalance();

	/**
	 * Yields the signature algorithm used by the account.
	 * 
	 * @return the signature algorithm used by the account
	 */
	SignatureAlgorithm getSignature();

	/**
	 * Yields the base58-encoded public key of the account.
	 * 
	 * @return the base58-encoded public key of the account
	 */
	String getPublicKeyBase58();

	/**
	 * Yields the base64-encoded public key of the account.
	 * 
	 * @return the base64-encoded public key of the account
	 */
	String getPublicKeyBase64();
}