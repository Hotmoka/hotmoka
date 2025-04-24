/*
Copyright 2021 Fausto Spoto

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.values.StorageReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "export",
	description = "Export a key pair file of an account as BIP39 words.",
	showDefaultValues = true)
public class Export extends AbstractCommand {

	@Parameters(index = "0", description = "the reference of the account to export in BIP39 words", converter = StorageReferenceOfAccountOptionConverter.class)
    private StorageReference reference;

	@Option(names = "--dir", description = "the path of the directory where the key pair of the account can be found", defaultValue = "")
    private Path dir;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	@Override
	protected void execute() throws CommandException {
        Account account;

        try {
        	account = Accounts.of(reference, dir);
        }
        catch (IOException e) {
        	throw new CommandException("Cannot read the key pair of the account: it was expected to be in file " + reference + ".pem", e);
        }

        if (json)
        	System.out.println(new Gson().toJson(account.bip39Words().stream().collect(Collectors.toList())));
        else {
        	var counter = new AtomicInteger(0);
        	account.bip39Words().stream().forEachOrdered(word -> System.out.printf("%2d: %s\n", counter.incrementAndGet(), word));
        }
	}
}