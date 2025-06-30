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

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.KeysShowOutputs;
import io.hotmoka.moka.api.keys.KeysShowOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.internal.json.KeysShowOutputJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "show", header = "Show information about a key pair.", showDefaultValues = true)
public class Show extends AbstractMokaCommand {

	@Parameters(index = "0", description = "the path of the file holding the key pair")
    private Path keys;

	@Option(names = "--password", description = "the password of the key pair", interactive = true, defaultValue = "")
    private char[] password;

	@Option(names = "--signature", description = "the signature algorithm for the key pair (ed25519, sha256dsa, qtesla1, qtesla3)",
			converter = SignatureOptionConverter.class, defaultValue = "ed25519")
	private SignatureAlgorithm signature;

	@Option(names = "--show-private", description = "show the private key")
	private boolean showPrivate;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	@Override
	protected void execute() throws CommandException {
		String passwordAsString = new String(password);

		try {
			KeyPair keys = Entropies.load(this.keys).keys(passwordAsString, signature);

			try {
				report(json, new Output(signature, keys, showPrivate), KeysShowOutputs.Encoder::new);
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The sha256 hashing algorithm is not available in this machine!");
			}
			catch (InvalidKeyException e) {
				// this should be impossible, since we have created the keys with the same signature algorithm
				throw new RuntimeException(e);
			}
		}
		catch (IOException e) {
			throw new CommandException("Cannot access file \"" + keys + "\"", e);
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements KeysShowOutput {
		private final SignatureAlgorithm signature;
		private final String publicKeyBase58;
		private final String publicKeyBase64;
		private final String tendermintAddress;
		private final String privateKeyBase58;
		private final String privateKeyBase64;
		private final String concatenatedBase64;

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 * @throws NoSuchAlgorithmException if {@code json} refers to a non-available cryptographic algorithm
		 */
		public Output(KeysShowOutputJson json) throws InconsistentJsonException, NoSuchAlgorithmException {
			ExceptionSupplierFromMessage<InconsistentJsonException> exp = InconsistentJsonException::new;
			this.signature = SignatureAlgorithms.of(Objects.requireNonNull(json.getSignature(), "signature cannot be null", exp));
			this.publicKeyBase58 = Base58.requireBase58(Objects.requireNonNull(json.getPublicKeyBase58(), "publicKeyBase58 cannot be null", exp), exp);
			this.publicKeyBase64 = Base64.requireBase64(Objects.requireNonNull(json.getPublicKeyBase64(), "publicKeyBase64 cannot be null", exp), exp);
			this.tendermintAddress = Hex.requireHex(Objects.requireNonNull(json.getTendermintAddress(), "tendermintAddress cannot be null", exp), exp);
			if ((this.privateKeyBase58 = json.getPrivateKeyBase58().orElse(null)) != null)
				Base58.requireBase58(privateKeyBase58, exp);
			if ((this.privateKeyBase64 = json.getPrivateKeyBase64().orElse(null)) != null)
				Base64.requireBase64(privateKeyBase64, exp);
			if ((this.concatenatedBase64 = json.getConcatenatedBase64().orElse(null)) != null)
				Base64.requireBase64(concatenatedBase64, exp);
		}

		private Output(SignatureAlgorithm signature, KeyPair keys, boolean alsoPrivate) throws NoSuchAlgorithmException, InvalidKeyException {
			this.signature = signature;
			byte[] publicKeyBytes = signature.encodingOf(keys.getPublic());
			this.publicKeyBase58 = Base58.toBase58String(publicKeyBytes);
			this.publicKeyBase64 = Base64.toBase64String(publicKeyBytes);

			byte[] sha256HashedKey = HashingAlgorithms.sha256().getHasher(Function.identity()).hash(publicKeyBytes);
			try {
				this.tendermintAddress = Hex.toHexString(sha256HashedKey, 0, 20).toUpperCase();
			}
			catch (HexConversionException e) {
				// this should not happen since the output of the sha256 algorithm can be converted into a hex string
				throw new RuntimeException("The output of the sha256 algorithm is invalid!", e);
			}

			if (alsoPrivate) {
				byte[] privateKeyBytes = signature.encodingOf(keys.getPrivate());
				var concatenated = new byte[privateKeyBytes.length + publicKeyBytes.length];
				System.arraycopy(privateKeyBytes, 0, concatenated, 0, privateKeyBytes.length);
				System.arraycopy(publicKeyBytes, 0, concatenated, privateKeyBytes.length, publicKeyBytes.length);
				this.privateKeyBase58 = Base58.toBase58String(privateKeyBytes);
				this.privateKeyBase64 = Base64.toBase64String(privateKeyBytes);
				this.concatenatedBase64 = Base64.toBase64String(concatenated);
			}
			else {
				this.privateKeyBase58 = null;
				this.privateKeyBase64 = null;
				this.concatenatedBase64 = null;
			}
		}

		@Override
		public SignatureAlgorithm getSignature() {
			return signature;
		}

		@Override
		public String getPublicKeyBase58() {
			return publicKeyBase58;
		}

		@Override
		public String getPublicKeyBase64() {
			return publicKeyBase64;
		}

		@Override
		public String getTendermintAddress() {
			return tendermintAddress;
		}

		@Override
		public Optional<String> getPrivateKeyBase58() {
			return Optional.ofNullable(privateKeyBase58);
		}

		@Override
		public Optional<String> getPrivateKeyBase64() {
			return Optional.ofNullable(privateKeyBase64);
		}

		@Override
		public Optional<String> getConcatenatedBase64() {
			return Optional.ofNullable(concatenatedBase64);
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			if (publicKeyBase58.length() > MAX_PRINTED_KEY)
				sb.append("* public key: " + publicKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)\n");
			else
				sb.append("* public key: " + publicKeyBase58 + " (" + signature + ", base58)\n");

			if (publicKeyBase64.length() > MAX_PRINTED_KEY)
				sb.append("* public key: " + publicKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)\n");
			else
				sb.append("* public key: " + publicKeyBase64 + " (" + signature + ", base64)\n");

			sb.append("* Tendermint-like address: " + tendermintAddress + "\n");

			if (privateKeyBase58 != null) {
				if (privateKeyBase58.length() > MAX_PRINTED_KEY)
					sb.append("* private key: " + privateKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)\n");
				else
					sb.append("* private key: " + privateKeyBase58 + " (" + signature + ", base58)\n");
			}

			if (privateKeyBase64 != null) {
				if (privateKeyBase64.length() > MAX_PRINTED_KEY)
					sb.append("* private key: " + privateKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)\n");
				else
					sb.append("* private key: " + privateKeyBase64 + " (" + signature + ", base64)\n");
			}

			if (concatenatedBase64 != null) {
				if (concatenatedBase64.length() > MAX_PRINTED_KEY * 2)
					sb.append("* concatenated private+public key: " + concatenatedBase64.substring(0, MAX_PRINTED_KEY * 2) + "..." + " (base64)\n");
				else
					sb.append("* concatenated private+public key: " + concatenatedBase64 + " (base64)\n");
			}

			return sb.toString();
		}
	}
}