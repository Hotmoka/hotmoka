package io.hotmoka.tools.internal.cli;

import static io.hotmoka.beans.types.ClassType.BIG_INTEGER;
import static io.hotmoka.beans.types.ClassType.GAMETE;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;

import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.ManifestHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "faucet",
	description = "Sets the thresholds for the faucet of the gamete of a node",
	showDefaultValues = true)
public class Faucet extends AbstractCommand {

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Parameters(description = "the maximal amount of coins sent at each call to the faucet of the node", defaultValue = "0")
    private BigInteger max;

	@Option(names = { "--max-red" }, description = "the maximal amount of red coins sent at each call to the faucet of the node", defaultValue = "0")
    private BigInteger maxRed;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;

		private Run() throws Exception {
			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				openFaucet();
			}
		}

		private void openFaucet() throws Exception {
			ManifestHelper manifestHelper = new ManifestHelper(node);
			StorageReference gamete = manifestHelper.gamete;
			KeyPair keys;

			try {
				keys = readKeys(gamete);
			}
			catch (IOException | ClassNotFoundException e) {
				System.err.println("Cannot read the keys of the gamete: they were expected to be stored in file " + fileFor(gamete));
				throw e;
			}

			// we set the thresholds for the faucets of the gamete
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(Signer.with(node.getSignatureAlgorithmForRequests(), keys),
				gamete, new NonceHelper(node).getNonceOf(gamete),
				manifestHelper.getChainId(), _10_000, new GasHelper(node).getGasPrice(), node.getTakamakaCode(),
				new VoidMethodSignature(GAMETE, "setMaxFaucet", BIG_INTEGER, BIG_INTEGER), gamete,
				new BigIntegerValue(max), new BigIntegerValue(maxRed)));
		}
	}
}