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

package io.hotmoka.tools.internal.moka;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;

import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.helpers.GasHelper;
import io.hotmoka.helpers.NonceHelper;
import io.hotmoka.helpers.SignatureHelper;
import io.hotmoka.nodes.Account;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "install",
	description = "Installs a jar in a node",
	showDefaultValues = true)
public class Install extends AbstractCommand {

	@Parameters(description = "the jar to install")
	private Path jar;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the call and becomes caller inside the method; it can be left blank for @View calls, in which case the manifest is used as caller")
	private String payer;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
	private String url;

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

			try (Node node = RemoteNode.of(remoteNodeConfig(url))) {
				TransactionReference takamakaCode = node.getTakamakaCode();
				StorageReference manifest = node.getManifest();
				checkStorageReference(payer);
				StorageReference payer = new StorageReference(Install.this.payer);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
				GasHelper gasHelper = new GasHelper(node);
				NonceHelper nonceHelper = new NonceHelper(node);
				byte[] bytes = Files.readAllBytes(jar);
				KeyPair keys = readKeys(new Account(payer), node, passwordOfPayer);
				TransactionReference[] dependencies;
				if (libs != null)
					dependencies = libs.stream().map(LocalTransactionReference::new).distinct().toArray(TransactionReference[]::new);
				else
					dependencies = new TransactionReference[] { takamakaCode };

				SignatureAlgorithm<SignedTransactionRequest> signature = new SignatureHelper(node).signatureAlgorithmFor(payer);
				BigInteger gas;
				if ("heuristic".equals(gasLimit))
					gas = _100_000.add(gasForTransactionWhosePayerHasSignature(signature.getName(), node)).add(BigInteger.valueOf(200).multiply(BigInteger.valueOf(bytes.length)));
				else
					gas = new BigInteger(gasLimit);

				TransactionReference classpath = "takamakaCode".equals(Install.this.classpath) ?
					takamakaCode : new LocalTransactionReference(Install.this.classpath);

				askForConfirmation(gas);

				JarStoreTransactionRequest request = new JarStoreTransactionRequest(
					Signer.with(signature, keys),
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