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

import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.GAMETE;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;

import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "faucet",
	description = "Set the thresholds for the faucet of the gamete of a node",
	showDefaultValues = true)
public class Faucet extends AbstractCommand {

	@Parameters(description = "the maximal amount of coins sent at each call to the faucet of the node", defaultValue = "0")
    private BigInteger max;

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Option(names = { "--password-of-gamete" }, description = "the password of the gamete account; if not specified, it will be asked interactively")
    private String passwordOfGamete;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true") 
	private boolean interactive;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;

		private Run() throws Exception {
			passwordOfGamete = ensurePassword(passwordOfGamete, "the gamete account", interactive, false);

			try (var node = this.node = RemoteNodes.of(uri, 10_000)) {
				openFaucet();
			}
		}

		private void openFaucet() throws Exception {
			StorageReference manifest = node.getManifest();
			var gamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, node.getTakamakaCode(), MethodSignatures.GET_GAMETE, manifest))
					.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAMETE + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_GAMETE, NodeException::new);

			String chainId = node.getConfig().getChainId();
			KeyPair keys;

			try {
				keys = readKeys(Accounts.of(gamete), node, passwordOfGamete);
			}
			catch (IOException | ClassNotFoundException e) {
				System.err.println("Cannot read the keys of the gamete: they were expected to be stored in file " + gamete + ".pem");
				throw e;
			}

			var takamakaCode = node.getTakamakaCode();

			// we set the thresholds for the faucets of the gamete
			node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
				(SignatureHelpers.of(node).signatureAlgorithmFor(gamete).getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
				gamete, NonceHelpers.of(node).getNonceOf(gamete),
				chainId, _100_000, GasHelpers.of(node).getGasPrice(), takamakaCode,
				MethodSignatures.ofVoid(GAMETE, "setMaxFaucet", BIG_INTEGER), gamete,
				StorageValues.bigIntegerOf(max)));
		}
	}
}