package io.hotmoka.tools.internal.cli;

import static io.hotmoka.beans.types.ClassType.BIG_INTEGER;
import static io.hotmoka.beans.types.ClassType.GAMETE;
import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

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
import io.hotmoka.remote.RemoteNodeConfig;

public class Faucet extends AbstractCommand {

	@Override
	public boolean run(CommandLine line) throws UncheckedException {
		if (line.hasOption("f")) {
			try {
				new Run(line);
			}
			catch (Exception e) {
				throw new UncheckedException(e);
			}

			return true;
		}
		else
			return false;
	}

	@Override
	public void populate(Options options) {
		options.addOption(Option.builder("f")
			.longOpt("faucet")
			.desc("sets the maximal amount of coins sent at each call to the faucet of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());
		
		options.addOption(Option.builder()
			.longOpt("max-faucet-red")
			.desc("specifies the maximal amount of red coins sent at each call to the faucet of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());

		options.addOption(Option.builder("url")
			.desc("specifies the url of the node that receives the command (such as http://my.machine.com:8080). Defaults to http://localhost:8080")
			.hasArg()
			.type(String.class)
			.build());
	}

	private class Run {
		private final Node node;

		private Run(CommandLine line) throws Exception {
			BigInteger maxFaucet = new BigInteger(line.getOptionValue("f"));
			BigInteger maxRedFaucet = getBigIntegerOption(line, "max-faucet-red", ZERO);
			String url = getStringOption(line, "url", "http://localhost:8080");

			RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder().setURL(url).build();

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig)) {
				openFaucet(maxFaucet, maxRedFaucet);
			}
		}

		private void openFaucet(BigInteger maxFaucet, BigInteger maxRedFaucet) throws Exception {
			ManifestHelper manifestHelper = new ManifestHelper(node);
			StorageReference gamete = manifestHelper.gamete;
			KeyPair keys;

			try {
				keys = readKeys(gamete);
			}
			catch (IOException | ClassNotFoundException e) {
				System.out.println("Cannot read the keys of the gamete: they were expected to be stored in file " + fileFor(gamete));
				throw e;
			}

			// we set the thresholds for the faucets of the gamete
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(Signer.with(node.getSignatureAlgorithmForRequests(), keys),
				gamete, new NonceHelper(node).getNonceOf(gamete),
				manifestHelper.getChainId(), _10_000, new GasHelper(node).getSafeGasPrice(), node.getTakamakaCode(),
				new VoidMethodSignature(GAMETE, "setMaxFaucet", BIG_INTEGER, BIG_INTEGER), gamete,
				new BigIntegerValue(maxFaucet), new BigIntegerValue(maxRedFaucet)));
		}
	}
}