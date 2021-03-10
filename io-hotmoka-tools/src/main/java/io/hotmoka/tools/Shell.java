package io.hotmoka.tools;

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
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

/**
 * An example that shows how to create a brand new Tendermint blockchain and publish a server bound to it.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.tools.Shell
 */
public class Shell {

	protected final static BigInteger _10_000 = BigInteger.valueOf(10_000L);

	public static void main(String[] args) throws Exception {
		new Shell(args);
	}

	private Shell(String[] args) throws Exception {
		Options options = createOptions();

		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("help"))
				printHelp(options);
			else if (line.hasOption("init"))
				new Init(line);
			else
				printHelp(options);
		}
		catch (ParseException e) {
	    	System.err.println("Syntax error: " + e.getMessage());
	    	printHelp(options);
	    }
	}

	private static void printHelp(Options options) {
		new HelpFormatter().printHelp("java " + Shell.class.getName(), options);
	}

	private class Init {
		private final ConsensusParams consensus;
		private final NodeServiceConfig networkConfig;
		private final TendermintBlockchainConfig nodeConfig;
		private final TendermintBlockchain node;
		private final InitializedNode initialized;
		private final ManifestHelper manifestHelper;
		private final NonceHelper nonceHelper;
		private final GasHelper gasHelper;

		private Init(CommandLine line) throws Exception {
			askForConfirmation();

			BigInteger green = line.hasOption("balance") ?
				new BigInteger(line.getOptionValue("balance"))
				:
				ZERO;

			BigInteger red = line.hasOption("balancered") ?
				new BigInteger(line.getOptionValue("balancered"))
				:
				ZERO;

			BigInteger maxFaucet = line.hasOption("maxfaucet") ?
				new BigInteger(line.getOptionValue("maxfaucet"))
				:
				ZERO;

			BigInteger maxRedFaucet = line.hasOption("maxredfaucet") ?
				new BigInteger(line.getOptionValue("maxredfaucet"))
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
			System.out.print("Do you really want to start a new node at this place (old blocks and store will be lost) [Y/N] ");
			String answer = System.console().readLine();
			if (!"Y".equals(answer))
				System.exit(0);
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
				System.out.println("\nThe keys of the gamete have been succesfully written into the file " + fileName + "\n");
			}
		}
	}

	private static Options createOptions() {
		Options options = new Options();

		options.addOption(Option.builder("init")
			.desc("initializes a new single-node blockchain")
			.build());

		options.addOption(Option.builder("balance")
			.desc("specifies the balance of the gamete of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());

		options.addOption(Option.builder("balancered")
			.desc("specifies the red balance of the gamete of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());

		options.addOption(Option.builder("maxfaucet")
			.desc("specifies the maximal amount of coins sent by the faucet of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());

		options.addOption(Option.builder("maxredfaucet")
			.desc("specifies the maximal amount of red coins sent by the faucet of the new node")
			.hasArg()
			.type(BigInteger.class)
			.build());

		options.addOption(Option.builder("help")
			.desc("print this help")
			.build());

		return options;
	}
}