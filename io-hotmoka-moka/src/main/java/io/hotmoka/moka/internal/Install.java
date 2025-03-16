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

package io.hotmoka.moka.internal;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "install",
	description = "Install a jar in a node",
	showDefaultValues = true)
public class Install extends AbstractCommand {

	@Parameters(description = "the jar to install")
	private Path jar;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for installing the jar")
	private String payer;

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Option(names = { "--password-of-payer" }, description = "the password of the payer account; if not specified, it will be asked interactively")
    private String passwordOfPayer;

	@Option(names = { "--libs" }, description = "the references of the dependencies of the jar, already installed in the node (takamakaCode is automatically added)")
	private List<String> libs;

	@Option(names = "--classpath", description = "the classpath used to interpret the payer", defaultValue = "takamakaCode")
    private String classpath;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true") 
	private boolean interactive;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for the installation", defaultValue = "heuristic") 
	private String gasLimit;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {

		private Run() throws Exception {
			passwordOfPayer = ensurePassword(passwordOfPayer, "the payer account", interactive, false);

			try (var node = RemoteNodes.of(uri, 10_000)) {
				var takamakaCode = node.getTakamakaCode();
				var manifest = node.getManifest();
				checkStorageReference(payer);
				var payer = StorageValues.reference(Install.this.payer, CommandException::new);
				String chainId = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest, StorageValues.NO_VALUES, IllegalArgumentException::new))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_CHAIN_ID + " should not return void"))
					.asReturnedString(MethodSignatures.GET_CHAIN_ID, NodeException::new);
				var gasHelper = GasHelpers.of(node);
				var nonceHelper = NonceHelpers.of(node);
				var bytes = Files.readAllBytes(jar);
				KeyPair keys = readKeys(Accounts.of(payer), node, passwordOfPayer);
				TransactionReference[] dependencies;
				if (libs != null) {
					var libsRefs = new ArrayList<TransactionReference>();
					for (String lib: libs)
						libsRefs.add(TransactionReferences.of(lib, s -> new CommandException("Library " + lib + " is not a valid transaction reference: " + s)));

					dependencies = libsRefs.stream().distinct().toArray(TransactionReference[]::new);
				}
				else
					dependencies = new TransactionReference[] { takamakaCode };

				var signature = SignatureHelpers.of(node).signatureAlgorithmFor(payer);
				BigInteger gas;
				if ("heuristic".equals(gasLimit))
					gas = _100_000.add(gasForTransactionWhosePayerHasSignature(signature.getName(), node)).add(BigInteger.valueOf(200).multiply(BigInteger.valueOf(bytes.length)));
				else
					gas = new BigInteger(gasLimit);

				TransactionReference classpath = "takamakaCode".equals(Install.this.classpath) ?
					takamakaCode : TransactionReferences.of(Install.this.classpath, s -> new CommandException("The classpath " + Install.this.classpath + " is not a valid transaction reference: " + s));

				askForConfirmation(gas);

				var request = TransactionRequests.jarStore(
					signature.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
					payer,
					nonceHelper.getNonceOf(payer),
					chainId,
					gas,
					gasHelper.getGasPrice(),
					classpath,
					bytes,
					dependencies);

				try {
					TransactionReference response = node.addJarStoreTransaction(request);
					System.out.println(jar + " has been installed at " + response);
				}
				finally {
					printCosts(node, request);
				}
			}
		}

		private void askForConfirmation(BigInteger gas) {
			if (interactive)
				yesNo("Do you really want to spend up to " + gas + " gas units to install the jar [Y/N] ");
		}
	}
}