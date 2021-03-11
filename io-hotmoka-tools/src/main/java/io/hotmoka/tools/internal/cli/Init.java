package io.hotmoka.tools.internal.cli;

import static io.hotmoka.beans.types.ClassType.BIG_INTEGER;
import static io.hotmoka.beans.types.ClassType.GAMETE;
import static java.math.BigInteger.ZERO;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.ManifestHelper;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;

public class Init implements Command {
	private final static BigInteger _10_000 = BigInteger.valueOf(10_000L);

	@Override
	public boolean run(CommandLine line) throws UncheckedException {
		if (line.hasOption("it")) {
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
		options.addOption(Option.builder("it")
			.longOpt("init-tendermint")
			.hasArg()
			.argName("balance")
			.type(BigInteger.class)
			.desc("initializes a new Hotmoka node based on Tendermint, whose gamete has the given initial balance")
			.build());
		
		options.addOption(Option.builder()
			.longOpt("non-interactive")
			.desc("does not ask for confirmation")
			.build());

		options.addOption(Option.builder()
			.longOpt("balance-red")
			.desc("specifies the red balance of the gamete of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());
		
		options.addOption(Option.builder()
			.longOpt("max-faucet")
			.desc("specifies the maximal amount of coins sent at each call to the faucet of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());
		
		options.addOption(Option.builder()
			.longOpt("max-faucet-red")
			.desc("specifies the maximal amount of red coins sent at each call to the faucet of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());
	}

	private static class Run {
		private final CommandLine line;
		private final ConsensusParams consensus;
		private final NodeServiceConfig networkConfig;
		private final TendermintBlockchainConfig nodeConfig;
		private final TendermintBlockchain node;
		private final InitializedNode initialized;
		private final ManifestHelper manifestHelper;
		private final NonceHelper nonceHelper;
		private final GasHelper gasHelper;

		private Run(CommandLine line) throws Exception {
			this.line = line;

			askForConfirmation();

			BigInteger green = new BigInteger(line.getOptionValue("it"));

			BigInteger red = line.hasOption("balance-red") ?
				new BigInteger(line.getOptionValue("balance-red"))
				:
				ZERO;

			BigInteger maxFaucet = line.hasOption("max-faucet") ?
				new BigInteger(line.getOptionValue("max-faucet"))
				:
				ZERO;

			BigInteger maxRedFaucet = line.hasOption("max-faucet-red") ?
				new BigInteger(line.getOptionValue("max-faucet-red"))
				:
				ZERO;

			nodeConfig = new TendermintBlockchainConfig.Builder()
				.setTendermintConfigurationToClone(Paths.get("io-hotmoka-tools/tendermint_configs/v1n0/node0"))
				.build();

			networkConfig = new NodeServiceConfig.Builder()
				.setSpringBannerModeOn(false)
				.build();

			consensus = new ConsensusParams.Builder()
				.allowUnsignedFaucet(maxFaucet.signum() > 0 || maxRedFaucet.signum() > 0)
				.build();

			Path takamakaCodeJar = Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar");

			try (TendermintBlockchain node = this.node = TendermintBlockchain.init(nodeConfig, consensus);
				InitializedNode initialized = this.initialized = TendermintInitializedNode.of(node, consensus, takamakaCodeJar, green, red);
				NodeService service = NodeService.of(networkConfig, node)) {

				manifestHelper = new ManifestHelper(node);
				nonceHelper = new NonceHelper(node);
				gasHelper = new GasHelper(node);

				openFaucet(maxFaucet, maxRedFaucet);
				dumpKeysOfGamete();
				printManifest();
				printBanner();
				waitForEnterKey();
			}
		}

		private void askForConfirmation() {
			if (!line.hasOption("non-interactive")) {
				System.out.print("Do you really want to start a new node at this place (old blocks and store will be lost) [Y/N] ");
				String answer = System.console().readLine();
				if (!"Y".equals(answer))
					System.exit(0);
			}
		}

		private void waitForEnterKey() {
			System.console().readLine();
		}

		private void printBanner() {
			System.out.println("The Hotmoka node has been published at localhost:" + networkConfig.port);
			System.out.println("Try for instance in a browser: http://localhost:" + networkConfig.port + "/get/manifest");
			System.out.println("\nPress enter to exit this program and turn off the node");
		}

		private void openFaucet(BigInteger maxFaucet, BigInteger maxRedFaucet) throws Exception {
			StorageReference gamete = initialized.gamete();

			// we set the thresholds for the faucets of the gamete
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(Signer.with(node.getSignatureAlgorithmForRequests(), initialized.keysOfGamete()),
				gamete, nonceHelper.getNonceOf(gamete), manifestHelper.getChainId(), _10_000, gasHelper.getSafeGasPrice(), node.getTakamakaCode(),
				new VoidMethodSignature(GAMETE, "setMaxFaucet", BIG_INTEGER, BIG_INTEGER), gamete,
				new BigIntegerValue(maxFaucet), new BigIntegerValue(maxRedFaucet)));
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException {
			System.out.println(manifestHelper);
		}

		private void dumpKeysOfGamete() throws TransactionRejectedException, TransactionException, CodeExecutionException, FileNotFoundException, IOException {
			StorageReference gamete = initialized.gamete();
			String fileName = gamete.toString() + ".keys";
		    
			try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
				oos.writeObject(initialized.keysOfGamete());
				System.out.println("\nThe keys of the gamete have been saved into the file " + fileName + "\n");
			}
		}
	}
}