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

package io.hotmoka.moka.internal.keys;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.function.Function;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * Information about a key pair.
 */
@Immutable
public class KeysInfo {
	public final String signature;
	public final String publicKeyBase58;
	public final String publicKeyBase64;
	public final String tendermintAddress;
	public final String privateKeyBase58;
	public final String privateKeyBase64;
	public final String concatenatedBase64;

	/**
	 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
	 */
	public final static int MAX_PRINTED_KEY = 200;

	public KeysInfo(SignatureAlgorithm signature, KeyPair keys, boolean alsoPrivate) throws CommandException {
		this.signature = signature.getName();

		try {
			byte[] publicKeyBytes = signature.encodingOf(keys.getPublic());
			byte[] privateKey = signature.encodingOf(keys.getPrivate());
			var concatenated = new byte[privateKey.length + publicKeyBytes.length];
			System.arraycopy(privateKey, 0, concatenated, 0, privateKey.length);
			System.arraycopy(publicKeyBytes, 0, concatenated, privateKey.length, publicKeyBytes.length);
			byte[] sha256HashedKey = HashingAlgorithms.sha256().getHasher(Function.identity()).hash(publicKeyBytes);
			this.publicKeyBase58 = Base58.toBase58String(publicKeyBytes);
			this.publicKeyBase64 = Base64.toBase64String(publicKeyBytes);

			if (alsoPrivate) {
				this.privateKeyBase58 = Base58.toBase58String(privateKey);
				this.privateKeyBase64 = Base64.toBase64String(privateKey);
				this.concatenatedBase64 = Base64.toBase64String(concatenated);
			}
			else {
				this.privateKeyBase58 = null;
				this.privateKeyBase64 = null;
				this.concatenatedBase64 = null;
			}

			this.tendermintAddress = Hex.toHexString(sha256HashedKey, 0, 20).toUpperCase();
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("The sha256 hashing algorithm is not available on this machine!", e);
		}
		catch (InvalidKeyException | HexConversionException e) {
			// this should not happen since we created the keys from the signature algorithm
			throw new RuntimeException("The new key pair is invalid!", e);
		}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof KeysInfo ki
			&& ki.signature.equals(signature)
			&& ki.publicKeyBase58.equals(publicKeyBase58)
			&& ki.publicKeyBase64.equals(publicKeyBase64)
			&& Objects.equals(ki.privateKeyBase58, privateKeyBase58)
			&& Objects.equals(ki.privateKeyBase64, privateKeyBase64)
			&& Objects.equals(ki.concatenatedBase64, concatenatedBase64);
	}

	@Override
	public String toString() {
		String result;

		if (publicKeyBase58.length() > MAX_PRINTED_KEY)
			result = "* public key: " + publicKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)";
		else
			result = "* public key: " + publicKeyBase58 + " (" + signature + ", base58)";

		if (publicKeyBase64.length() > MAX_PRINTED_KEY)
			result += "\n* public key: " + publicKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)";
		else
			result += "\n* public key: " + publicKeyBase64 + " (" + signature + ", base64)";

		result += "\n* Tendermint-like address: " + tendermintAddress;

		if (privateKeyBase58 != null) {
			if (privateKeyBase58.length() > MAX_PRINTED_KEY)
				result += "\n* private key: " + privateKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)";
			else
				result += "\n* private key: " + privateKeyBase58 + " (" + signature + ", base58)";
		}

		if (privateKeyBase64 != null) {
			if (privateKeyBase64.length() > MAX_PRINTED_KEY)
				result += "\n* private key: " + privateKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)";
			else
				result += "\n* private key: " + privateKeyBase64 + " (" + signature + ", base64)";
		}

		if (concatenatedBase64 != null) {
			if (concatenatedBase64.length() > MAX_PRINTED_KEY * 2)
				result += "\n* concatenated private+public key: " + concatenatedBase64.substring(0, MAX_PRINTED_KEY * 2) + "..." + " (base64)";
			else
				result += "\n* concatenated private+public key: " + concatenatedBase64 + " (base64)";
		}

		return result;
	}
}