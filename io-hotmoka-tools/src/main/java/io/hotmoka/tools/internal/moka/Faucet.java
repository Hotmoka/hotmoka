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

import static io.hotmoka.beans.types.ClassType.BIG_INTEGER;
import static io.hotmoka.beans.types.ClassType.GAMETE;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;

import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.Node;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "faucet",
	description = "Set the thresholds for the faucet of the gamete of a node",
	showDefaultValues = true)
public class Faucet extends AbstractCommand {

	@Parameters(description = "the maximal amount of coins sent at each call to the faucet of the node", defaultValue = "0")
    private BigInteger max;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
	private String url;

	@Option(names = { "--password-of-gamete" }, description = "the password of the gamete account; if not specified, it will be asked interactively")
    private String passwordOfGamete;

	@Option(names = { "--max-red" }, description = "the maximal amount of red coins sent at each call to the faucet of the node", defaultValue = "0")
    private BigInteger maxRed;

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

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				openFaucet();
			}
		}

		private void openFaucet() throws Exception {
			var manifestHelper = ManifestHelpers.of(node);
			StorageReference gamete = manifestHelper.getGamete();
			KeyPair keys;

			try {
				keys = readKeys(Accounts.of(gamete), node, passwordOfGamete);
			}
			catch (IOException | ClassNotFoundException e) {
				System.err.println("Cannot read the keys of the gamete: they were expected to be stored in file " + gamete + ".pem");
				throw e;
			}

			// we set the thresholds for the faucets of the gamete
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(SignatureHelpers.of(node).signatureAlgorithmFor(gamete).getSigner(keys.getPrivate()),
				gamete, NonceHelpers.of(node).getNonceOf(gamete),
				manifestHelper.getChainId(), _100_000, GasHelpers.of(node).getGasPrice(), node.getTakamakaCode(),
				new VoidMethodSignature(GAMETE, "setMaxFaucet", BIG_INTEGER, BIG_INTEGER), gamete,
				new BigIntegerValue(max), new BigIntegerValue(maxRed)));
		}
	}
}