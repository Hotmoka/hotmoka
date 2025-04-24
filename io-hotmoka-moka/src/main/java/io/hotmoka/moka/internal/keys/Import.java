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
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.BIP39Dictionaries;
import io.hotmoka.crypto.BIP39Mnemonics;
import io.hotmoka.node.Accounts;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "import",
	description = "Import the key pair file of an account from BIP39 words.",
	showDefaultValues = true)
public class Import extends AbstractCommand {

	@Parameters(description = "the 36 BIP39 words of the account to import")
    private List<String> words = new ArrayList<>();

	@Option(names = "--dir", description = "the directory where the key pair file of the account must be written", defaultValue = "")
    private Path dir;

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
		Path file = dir.resolve(account.getReference() + ".pem");

		try {
			account.dump(file);
		}
		catch (IOException e) {
			throw new CommandException("Could not write the key pair file of the account into " + file + ".pem", e);
		}

		if (json)
			System.out.println(new Gson().toJson(account.toString()));
		else
			System.out.println("The key pair of the account has been imported into the file \"" + file + "\".");
	}
}