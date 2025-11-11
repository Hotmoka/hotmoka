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

package io.hotmoka.moka.internal;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * The specification of the public key of an account: either
 * explicitly (in Base58 format) or as the path of a key pair.
 */
public class PublicKeyOrKeyPair {
	private final String key;
	private final Path keys;

	public PublicKeyOrKeyPair(String s) throws InvalidPathException {
		String key;
		Path keys;

		try {
			key = Base58.requireBase58(s, Base58ConversionException::new);
			keys = null;
		}
		catch (Base58ConversionException e) {
			key = null;
			keys = Paths.get(s);
		}

		this.key = key;
		this.keys = keys;
	}

	/**
	 * Yields the public key from this identifier.
	 * 
	 * @param signature the signature algorithm of the public key
	 * @param password the password of the key pair, if {@code --keys} is specified
	 * @return the public key
	 * @throws CommandException if some option is incorrect
	 */
	public PublicKey getPublicKey(SignatureAlgorithm signature, String password) throws CommandException {
		if (key != null) {
			try {
				return signature.publicKeyFromEncoding(Base58.fromBase58String(key, IllegalArgumentException::new));
			}
			catch (InvalidKeySpecException e) {
				throw new CommandException("The public key specified by --key is invalid for the signature algorithm " + signature, e);
			}
		}
		else {
			try {
				return Entropies.load(keys).keys(password, signature).getPublic();
			}
			catch (IOException e) {
				throw new CommandException("Cannot access file \"" + keys + "\"!", e);
			}
		}
	}

	/**
	 * Yields the public key from this identifier, in Base64 format.
	 * 
	 * @param signature the signature algorithm of the public key
	 * @param password the password of the key pair, if {@code --keys} is specified
	 * @return the public key, in Base64 format
	 * @throws CommandException if some option is incorrect
	 */
	public String getPublicKeyBase64(SignatureAlgorithm signature, String password) throws CommandException {
		try {
			return Base64.toBase64String(signature.encodingOf(getPublicKey(signature, password)));
		}
		catch (InvalidKeyException e) {
			// the key has been created with the same signature algorithm, it cannot be invalid
			throw new CommandException("The key seems invalid for the required signature algorithm", e);
		}
	}

	/**
	 * Yields the Base58-encoded public key of the account, if it was specified.
	 * 
	 * @return the Base58-encoded public key, if specified
	 */
	public Optional<String> getKeyBase58() {
		return Optional.ofNullable(key);
	}

	/**
	 * Yields the path of the key pair where the public key is contained, if it was specified.
	 * 
	 * @return the path, if specified
	 */
	public Optional<Path> getPathOfKeyPair() {
		return Optional.ofNullable(keys);
	}
}