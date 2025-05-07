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
import java.util.ArrayList;
import java.util.List;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.BIP39Dictionaries;
import io.hotmoka.crypto.BIP39Mnemonics;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.KeysImportOutputs;
import io.hotmoka.moka.api.keys.KeysImportOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.json.KeysImportOutputJson;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "import",
	header = "Import the key pair file of an account from BIP39 words.",
	showDefaultValues = true)
public class Import extends AbstractMokaCommand {

	@Parameters(description = "the 36 BIP39 words of the account to import")
    private List<String> words = new ArrayList<>();

	@Option(names = "--output-dir", paramLabel = "<path>", description = "the directory where the key pair file of the account will be written", defaultValue = "")
    private Path outputDir;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	@Override
	protected void execute() throws CommandException {
		if (words.size() != 36)
			throw new CommandException("Hotmoka accounts are represented by 36 BIP39 words, but " + words.size() + " words have been provided instead");

		for (String word: words)
			if (BIP39Dictionaries.ENGLISH_DICTIONARY.getAllWords().noneMatch(word::equals))
				throw new CommandException("The word \"" + word + "\" does not exist in the BIP39 English dictionary");

		var account = BIP39Mnemonics.of(words.toArray(String[]::new)).toAccount(Accounts::of);

		Path file = outputDir.resolve(account + ".pem");
		try {
			account.dump(file);
		}
		catch (IOException e) {
			throw new CommandException("Could not write the key pair file of the account into " + file + ".pem", e);
		}

		report(json, new Output(file, account.getReference()), KeysImportOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements KeysImportOutput {
		private final Path file;
		private final StorageReference account;

		private Output(Path file, StorageReference account) {
			this.file = file;
			this.account = account;
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

			this.account = Objects.requireNonNull(json.getAccount(), "account cannot be null", InconsistentJsonException::new).unmap()
				.asReference(value -> new InconsistentJsonException("The reference of the imported account must be a storage reference, not a " + value.getClass().getName()));
		}

		@Override
		public Path getFile() {
			return file;
		}

		@Override
		public StorageReference getAccount() {
			return account;
		}

		@Override
		public String toString() {
			return "The key pair of the account has been imported into " + asPath(file) + ".\n";
		}
	}
}