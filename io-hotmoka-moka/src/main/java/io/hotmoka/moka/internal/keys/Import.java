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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.BIP39Dictionaries;
import io.hotmoka.crypto.BIP39Mnemonics;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.KeysImportOutputs;
import io.hotmoka.moka.api.keys.KeysImportOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.internal.json.KeysImportOutputJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "import",
	header = "Import a key pair file from BIP39 words.",
	showDefaultValues = true)
public class Import extends AbstractMokaCommand {

	@Parameters(description = "the 12 BIP39 words of the key pair to import")
    private List<String> words = new ArrayList<>();

	@Option(names = "--output-dir", paramLabel = "<path>", description = "the directory where the key pair file will be written", defaultValue = "")
    private Path outputDir;

	@Option(names = "--name", description = "the name of the file where the new key pair must be written; if missing, the first characters of the Base58-encoded public key will be used, followed by \".pem\"")
    private String name;

	@Option(names = "--password", description = "the password of the key pair; this is only used if --name is not specified", interactive = true, defaultValue = "")
    private char[] password;

	@Option(names = "--signature", description = "the signature algorithm for the key pair (ed25519, sha256dsa, qtesla1, qtesla3); this is only used if --name is not specified",
			converter = SignatureOptionConverter.class, defaultValue = "ed25519")
	private SignatureAlgorithm signature;

	@Override
	protected void execute() throws CommandException {
		if (words.size() != 12)
			throw new CommandException("Hotmoka key pairs are represented by 12 BIP39 words, but " + words.size() + " words have been provided instead");

		for (String word: words)
			if (BIP39Dictionaries.ENGLISH_DICTIONARY.getAllWords().noneMatch(word::equals))
				throw new CommandException("The word \"" + word + "\" does not exist in the BIP39 English dictionary");

		var bytes = BIP39Mnemonics.of(words.toArray(String[]::new)).getBytes();
		var entropy = Entropies.of(bytes);

		var passwordAsString = new String(password);

		try {
			KeyPair keys = entropy.keys(passwordAsString, signature);

			String name = this.name;
			if (name == null) {
				try {
					name = Base58.toBase58String(signature.encodingOf(keys.getPublic()));
					if (name.length() > 100)
						name = name.substring(0, 100);
				}
				catch (InvalidKeyException e) {
					throw new CommandException("The key pair is invalid", e);
				}

				name = name + ".pem";
			}

			Path file = outputDir.resolve(name);
			try {
				entropy.dump(file);
			}
			catch (IOException e) {
				throw new CommandException("Could not write the key pair file into " + file + ".pem", e);
			}

			report(new Output(file), KeysImportOutputs.Encoder::new);
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements KeysImportOutput {
		private final Path file;

		private Output(Path file) {
			this.file = file;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(KeysImportOutputJson json) throws InconsistentJsonException {
			try {
				this.file = Paths.get(Objects.requireNonNull(json.getFile(), "file cannot be null", InconsistentJsonException::new));
			}
			catch (InvalidPathException e) {
				throw new InconsistentJsonException(e);
			}
		}

		@Override
		public Path getFile() {
			return file;
		}

		@Override
		public String toString() {
			return "The key pair has been imported into " + asPath(file) + ".\n";
		}
	}
}