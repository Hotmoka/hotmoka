/*
Copyright 2025 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.50000000000
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tutorial;

import static io.hotmoka.constants.Constants.HOTMOKA_VERSION;
import static io.mokamint.constants.Constants.MOKAMINT_VERSION;
import static io.takamaka.code.constants.Constants.TAKAMAKA_VERSION;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.LogManager;

import io.hotmoka.crypto.cli.keys.KeysCreateOutputs;
import io.hotmoka.moka.AccountsCreateOutputs;
import io.hotmoka.moka.AccountsSendOutputs;
import io.hotmoka.moka.JarsInstallOutputs;
import io.hotmoka.moka.Moka;
import io.hotmoka.moka.NodesManifestAddressOutputs;
import io.hotmoka.moka.NodesTakamakaAddressOutputs;
import io.hotmoka.moka.NodesTendermintInitOutputs;
import io.hotmoka.moka.ObjectsCallOutputs;
import io.hotmoka.moka.ObjectsCreateOutputs;
import io.hotmoka.moka.ObjectsShowOutputs;
import io.hotmoka.moka.api.jars.JarsInstallOutput;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.UpdateOfStorage;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.tutorial.examples.runs.Auction;
import io.hotmoka.tutorial.examples.runs.Decorators;
import io.hotmoka.tutorial.examples.runs.Events;
import io.hotmoka.tutorial.examples.runs.Family;
import io.hotmoka.tutorial.examples.runs.FamilyExported;
import io.hotmoka.tutorial.examples.runs.FamilyStorage;
import io.takamaka.code.constants.Constants;

/**
 * This executable runs experiments against a couple of remote Hotmoka nodes (one for
 * Mokamint and one for Tendermint) and reports the results inside latex files,
 * so that the recompilation of the tutorial will embed the exact results
 * of the experiments.
 * 
 * Run it with:
 * 
 * mvn exec:exec -Dexec.executable="java" -Dexec.args="-cp %classpath io.hotmoka.tutorial.UpdateForNewNode"
 */
public class UpdateForNewNode {

	/**
	 * The timeout (in milliseconds) used for the communication to the remote nodes. Better to use a relatively high
	 * value, since the Mokamint node has a varying block creation rate.
	 */
	public final static int TIMEOUT = 400000;

	/**
	 * The name where Latex commands are generated.
	 */
	public static final String LATEX_FILE_NAME = "parameters.tex";

	/**
	 * The name where Latex commands are generated, for the experiments with the Mokamint node.
	 */
	public static final String LATEX_MOKAMINT_FILE_NAME = "parameters_mokamint.tex";

	/**
	 * The name where Latex commands are generated, for the experiments with the Mokamint node and quantum-resistant keys.
	 */
	public static final String LATEX_MOKAMINT_QUANTUM_FILE_NAME = "parameters_quantum_mokamint.tex";

	/**
	 * The name where Latex commands are generated, for the experiments with the Tendermint node.
	 */
	public static final String LATEX_TENDERMINT_FILE_NAME = "parameters_tendermint.tex";

	/**
	 * The repository where the source code of Hotmoka is available.
	 */
	private final static String HOTMOKA_REPOSITORY = "https://github.com/Hotmoka/hotmoka";

	/**
	 * The web page of the Hotmoka project.
	 */
	private final static String HOTMOKA_WEB = "https://www.hotmoka.io";

	/**
	 * The directory where the tutorial suggests to install the projects.
	 */
	private final static String HOTMOKA_TUTORIAL_DIR = "hotmoka_tutorial";

	/**
	 * The version of Tendermint expected to be used for the experiments.
	 */
	private final static String TENDERMINT_VERSION = "0.34.15";

	/**
	 * The home directory of the user of this program.
	 */
	private final static Path HOME = Paths.get(System.getProperty("user.home"));

	private static URI mokamintServer;
	private static URI mokamintServerMining;
	private static URI mokamintServerPublic;
	private static URI tendermintServer;

	private UpdateForNewNode() {}

	/**
	 * Edit the {@code parameters.tex} file by rerunning the experiments of the Hotmoka tutorial.
	 * It allows one to specify the name of the directory where the files will be created and four further arguments,
	 * that are the URI of the remote nodes (for Mokamint and for Tendermint). The first defaults to
	 * {@code src/main/latex/} and the four two default to {@code ws://panarea.hotmoka.io:8001/8025/8030}
	 * and {@code ws://panarea.hotmoka.io:8002}.
	 * 
	 * @param args the arguments
	 * @throws Exception if the editing of the file fails for some reason
	 */
	public static void main(String[] args) throws Exception {
		Path outputDir = args.length > 0 ? Paths.get(args[0]) : Paths.get("src/main/latex/generated");
		mokamintServer = new URI(args.length > 1 ? args[1] : "ws://panarea.hotmoka.io:8001");
		mokamintServerMining = new URI(args.length > 2 ? args[2] : "ws://panarea.hotmoka.io:8025");
		mokamintServerPublic = new URI(args.length > 3 ? args[3] : "ws://panarea.hotmoka.io:8030");
		tendermintServer = new URI(args.length > 4 ? args[4] : "ws://panarea.hotmoka.io:8002");
		Path tempDir = Files.createTempDirectory("tmp");
		System.out.println("Saving temporary files inside the directory " + tempDir);

		// you can comment out some of the following lines if you only want to regenerate a subset of the experiments
		new ExperimentsWithoutServer(outputDir, tempDir);
		new ExperimentsQuantumWithMokamintServer(outputDir, tempDir);
		new ExperimentsWithMokamintServer(outputDir, tempDir);
		new ExperimentsWithTendermintServer(outputDir, tempDir);
	}

	/**
	 * Generates Latex files and commands that do not need to contact a remote server.
	 */
	private static class ExperimentsWithoutServer extends Experiments {
		private ExperimentsWithoutServer(Path outputDir, Path tempDir) throws Exception {
			super(outputDir, LATEX_FILE_NAME, tempDir);
		}

		@Override
		protected void generateFiles() throws Exception {
			report("hotmokaVersion", HOTMOKA_VERSION);
			report("mokamintVersion", MOKAMINT_VERSION);
			report("takamakaVersion", TAKAMAKA_VERSION);
			report("tendermintVersion", TENDERMINT_VERSION);
			report("faustoEmail", "\\email{fausto.spoto@hotmoka.io}");
			report("infoEmail", "\\email{info@hotmoka.io}");
			report("serverMokamint", mokamintServer.toString());
			report("serverMokamintMining", mokamintServerMining.toString());
			report("serverMokamintPublic", mokamintServerPublic.toString());
			report("serverTendermint", tendermintServer.toString());
			report("hotmokaRepo", "\\website{" + HOTMOKA_REPOSITORY + "}");
			report("hotmokaRepoReleases", "\\website{" + HOTMOKA_REPOSITORY + "/releases}");
			report("hotmokaWeb", "\\website{" + HOTMOKA_WEB + "}");
			report("hotmokaTutorialDir", HOTMOKA_TUTORIAL_DIR.replace("_", "\\_"));

			createCommandFile("git_clone_hotmoka", "git clone --branch v" + HOTMOKA_VERSION + " " + HOTMOKA_REPOSITORY);
			createCommandFile("mvn_clean_install", "mvn clean install");
			createOutputFile("moka_help", Moka.help(""));
			createCommandFile("moka_version", "moka --version");
			createOutputFile("moka_help_objects", Moka.help("objects"));
			createOutputFile("moka_objects_help_show", Moka.objectsHelp("show"));

			createCommandFile("docker_run_moka", "docker run -it --rm hotmoka/mokamint-node:" + HOTMOKA_VERSION + " moka --version");
			createCommandFile("install_moka", "mkdir -p ~/Gits\ncd ~/Gits\ngit clone --branch v" + HOTMOKA_VERSION + " " + HOTMOKA_REPOSITORY + "\ncd hotmoka\nmvn clean install -DskipTests");
			createCommandFile("export_path_moka", "export PATH=~/Gits/hotmoka/io-hotmoka-moka/src/main/bash:$PATH");

			createCommandFile("moka_jars_verify_takamaka", "moka jars verify ~/.m2/repository/io/hotmoka/io-takamaka-code/"
					+ TAKAMAKA_VERSION + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar --init");
			createOutputFile("moka_jars_verify_takamaka", Moka.jarsVerify(HOME + "/.m2/repository/io/hotmoka/io-takamaka-code/"
					+ TAKAMAKA_VERSION + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar --init"));

			createCommandFile("tendermint_version", "tendermint version");
			createOutputFile("tendermint_version", shell("tendermint version"));
			createCommandFile("tendermint_testnet", "tendermint testnet --v 1 --n 0");
			createOutputFile("tendermint_testnet", shell("tendermint testnet --v 1 --n 0"));
			var output1 = KeysCreateOutputs.from(Moka.keysCreate("--name gamete.pem --output-dir=" + tempDir + " --password=chocolate --json"));
			createCommandFile("moka_keys_create_gamete", "moka keys create --name=gamete.pem --password");
			createOutputFile("moka_keys_create_gamete", "Enter value for --password (the password that will be needed later to use the key pair): mypassword\n" + output1);
			report("gametePublicKeyBaseFiftyeight", output1.getPublicKeyBase58());

			createCommandFile("moka_nodes_tendermint_init", "moka nodes tendermint init ~/.m2/repository/io/hotmoka/io-takamaka-code/"
					+ TAKAMAKA_VERSION + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar --public-key-of-gamete=" + output1.getPublicKeyBase58()
					+ " --tendermint-config=mytestnet/node0");
			var output2 = NodesTendermintInitOutputs.from(Moka.nodesTendermintInit(HOME.resolve(".m2/repository/io/hotmoka/io-takamaka-code/" + TAKAMAKA_VERSION + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar")
					+ " --public-key-of-gamete=" + output1.getPublicKeyBase58()
					+ " --tendermint-config=" + tempDir.resolve("mytestnet/node0")
					+ " --chain-dir=" + tempDir.resolve("chain-tendermint")
					+ " --yes --exit-after-initialization --json"));
			createOutputFile("moka_nodes_tendermint_init", output2.toString());

			createCommandFile("moka_keys_bind_gamete", "moka keys bind gamete.pem --password");
			StorageReference gamete = output2.getGamete();
			createOutputFile("moka_keys_bind_gamete", "Enter value for --password (the password of the key pair): mypassword\n"
					+ "The key pair of " + gamete + " has been saved as \"" + gamete + ".pem\".");

			createCommandFile("moka_nodes_faucet", "moka nodes faucet 5000000 --password");
			createOutputFile("moka_nodes_faucet", "Enter value for --password (the password of the key pair): mypassword\nThe threshold of the faucet has been set.");

			createCommandFile("moka_nodes_tendermint_resume", "moka nodes tendermint resume");
			createOutputFile("moka_nodes_tendermint_resume", Moka.nodesTendermintResume("--chain-dir=" + tempDir.resolve("chain-tendermint") + " --exit-after-initialization"));

			createCommandFile("moka_nodes_disk_init", "moka nodes disk init ~/.m2/repository/io/hotmoka/io-takamaka-code/"
					+ TAKAMAKA_VERSION + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar --public-key-of-gamete=" + output1.getPublicKeyBase58()
					+ " --open-unsigned-faucet");
			var output3 = Moka.nodesDiskInit(HOME.resolve(".m2/repository/io/hotmoka/io-takamaka-code/" + TAKAMAKA_VERSION + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar")
					+ " --public-key-of-gamete=" + output1.getPublicKeyBase58()
					+ " --chain-dir=" + tempDir.resolve("chain-disk")
					+ " --open-unsigned-faucet --yes --exit-after-initialization");
			createOutputFile("moka_nodes_disk_init", output3);

			createCommandFile("tree_chain", "tree chain/hotmoka/store");
			var storeDir = tempDir.resolve("chain-disk").resolve("hotmoka").resolve("store");
			createOutputFile("tree_chain", shell("tree " + storeDir + " --noreport --charset=ascii").replace("chain-disk", "chain"));

			createCommandFile("cat_gamete_creation_request", "cat chain/hotmoka/store/b2/0-*/request.txt");
			createOutputFile("cat_gamete_creation_request", shell("cat " + storeDir.resolve("b2").resolve("0-*").resolve("request.txt")));
			createCommandFile("cat_gamete_creation_response", "cat chain/hotmoka/store/b2/0-*/response.txt");
			createOutputFile("cat_gamete_creation_response", shell("cat " + storeDir.resolve("b2").resolve("0-*").resolve("response.txt")));

			createCommandFile("cat_hotmoka_log", "cat ~/hotmoka.log.0 | tail -10");
			createOutputFile("cat_hotmoka_log", shell("cat " + HOME.resolve("hotmoka.log.0") + " | tail -10"));

			var instrumented = tempDir.resolve("instrumented");
			Files.createDirectory(instrumented);
			createCommandFile("moka_jars_instrument_takamaka", "mkdir instrumented\nmoka jars instrument ~/.m2/repository/io/hotmoka/io-takamaka-code/"
					+ TAKAMAKA_VERSION + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar instrumented/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar --init");
			createOutputFile("moka_jars_instrument_takamaka", Moka.jarsInstrument(HOME + "/.m2/repository/io/hotmoka/io-takamaka-code/"
					+ TAKAMAKA_VERSION + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar "
					+ instrumented.resolve("io-takamaka-code-" + TAKAMAKA_VERSION + ".jar") + " --init"));

			createCommandFile("moka_jars_instrument_family", "moka jars instrument ~/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/"
					+ HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION
					+ ".jar instrumented/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION
					+ ".jar --libs instrumented/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar");
			createOutputFile("moka_jars_instrument_family", Moka.jarsInstrument(HOME + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/"
					+ HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar "
					+ instrumented.resolve("io-hotmoka-tutorial-examples-family-" + TAKAMAKA_VERSION + ".jar")
					+ " --libs " + instrumented.resolve("io-takamaka-code-" + TAKAMAKA_VERSION + ".jar")));

			createCommandFile("moka_jars_verify_family_errors", "moka jars verify ~/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_errors/"
					+ HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_errors-" + HOTMOKA_VERSION
					+ ".jar --libs instrumented/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar");
			createOutputFile("moka_jars_verify_family_errors", Moka.jarsVerify(HOME + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_errors/"
					+ HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_errors-" + HOTMOKA_VERSION + ".jar --libs "
					+ instrumented.resolve("io-takamaka-code-" + TAKAMAKA_VERSION + ".jar")));

			createCommandFile("moka_jars_instrument_family_errors", "moka jars instrument ~/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_errors/"
					+ HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_errors-" + HOTMOKA_VERSION
					+ ".jar instrumented/io-hotmoka-tutorial-examples-family_errors-" + HOTMOKA_VERSION
					+ ".jar --libs instrumented/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar");
			createOutputFile("moka_jars_instrument_family_errors", Moka.jarsInstrument(HOME + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_errors/"
					+ HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_errors-" + HOTMOKA_VERSION + ".jar "
					+ instrumented.resolve("io-hotmoka-tutorial-examples-family_errors-" + HOTMOKA_VERSION + ".jar")
					+ " --libs " + instrumented.resolve("io-takamaka-code-" + TAKAMAKA_VERSION + ".jar")));

			createCommandFile("mvn_exec_decorators", "cd io-hotmoka-tutorial-examples-runs\nmvn clean install exec:exec -Dexec.executable=\"java\" -Dexec.args=\"-cp %classpath io.hotmoka.tutorial.examples.runs.Decorators\"");
			String runDecoratorsMain = run(() -> Decorators.main(new String[] {}));
			createOutputFile("mvn_exec_decorators", runDecoratorsMain);

			createCommandFile("mvn_exec_publisher", "cd io-hotmoka-tutorial-examples-runs\nmvn clean install exec:exec -Dexec.executable=\"java\" -Dexec.args=\"-cp %classpath io.hotmoka.tutorial.examples.runs.Publisher\"");

			var output4 = KeysCreateOutputs.from(Moka.keysCreate("--name miner.pem --output-dir=" + tempDir + " --password=sun --json"));
			createCommandFile("docker_create_keys_for_mining", "docker run --name temp -it mokamint/mokamint:" + MOKAMINT_VERSION + " mokamint-node keys create --name miner.pem --password; docker cp temp:/home/mokamint/miner.pem .; docker rm temp");
			createOutputFile("docker_create_keys_for_mining", "Enter value for --password (the password that will be needed later to use the key pair): sun\n" + output4);			

			String minerPublicKeyBase58 = output4.getPublicKeyBase58();
			createCommandFile("docker_config_miner", "docker run --log-driver local --rm -it -e MINER_PUBLIC_SERVICE_URI=" + mokamintServerMining + " -e PUBLIC_KEY_MINER_BASE58=" + minerPublicKeyBase58 + " -e PLOT_SIZE=\"5000\" -v miner_configuration:/home/mokamint/miner_configuration mokamint/mokamint:" + MOKAMINT_VERSION + " config-miner");

			createCommandFile("docker_mine", "docker run --log-driver local --name miner --rm -it -e MINER_PUBLIC_SERVICE_URI=" + mokamintServerMining + " -v miner_configuration:/home/mokamint/miner_configuration mokamint/mokamint:" + MOKAMINT_VERSION + " mine");

			createCommandFile("docker_check_balance", "docker run --rm -it mokamint/mokamint:" + MOKAMINT_VERSION + " mokamint-miner balance " + minerPublicKeyBase58 + " --uri " + mokamintServerMining);

			createCommandFile("moka_keys_bind_miner", "moka keys bind miner.pem --password --uri=" + mokamintServer);

			createCommandFile("docker_mokamint_config_clone", "docker run -it --rm -e PUBLIC_KEY_MINER_BASE58=" + minerPublicKeyBase58 + " -e MOKAMINT_PUBLIC_SERVICE_URI=" + mokamintServerPublic + " -e PLOT_SIZE=4000 -v chain:/home/hotmoka/chain -v hotmoka_mokamint:/home/hotmoka/hotmoka_mokamint hotmoka/mokamint-node:" + HOTMOKA_VERSION + " config-clone");

			createCommandFile("docker_mokamint_go", "docker run -it --log-driver local --rm --name hotmoka -p 8001:8001 -p 8025:8025 -p 8030:8030 -p 127.0.0.1:8031:8031 -v chain:/home/hotmoka/chain -v hotmoka_mokamint:/home/hotmoka/hotmoka_mokamint hotmoka/mokamint-node:" + HOTMOKA_VERSION + " go");

			var output5 = KeysCreateOutputs.from(Moka.keysCreate("--name gamete.pem --output-dir=" + tempDir + " --password=moon --json"));
			createCommandFile("docker_create_keys_for_mokamint_gamete", "docker run --name temp -it hotmoka/mokamint-node:" + HOTMOKA_VERSION + " moka keys create --name gamete.pem --password; docker cp temp:/home/hotmoka/gamete.pem .; docker rm temp");
			createOutputFile("docker_create_keys_for_mokamint_gamete", "Enter value for --password (the password that will be needed later to use the key pair): moon\n" + output5);			

			String gametePublicKeyBase64 = output5.getPublicKeyBase64();
			createCommandFile("docker_mokamint_config_new", "docker run -it --rm -e PUBLIC_KEY_MINER_BASE58=" + minerPublicKeyBase58 + " -e PUBLIC_KEY_GAMETE_BASE64=\"" + gametePublicKeyBase64 + "\" -e PLOT_SIZE=5000 -e CHAIN_ID=whale -e TARGET_BLOCK_CREATION_TIME=10000 -v chain:/home/hotmoka/chain -v hotmoka_mokamint:/home/hotmoka/hotmoka_mokamint hotmoka/mokamint-node:" + HOTMOKA_VERSION + " config-new");

			createCommandFile("docker_mokamint_init", "docker run -it --rm -v chain:/home/hotmoka/chain -v hotmoka_mokamint:/home/hotmoka/hotmoka_mokamint hotmoka/mokamint-node:" + HOTMOKA_VERSION + " init");

			createCommandFile("docker_tendermint_config_clone", "docker run -it --rm -e HOTMOKA_PUBLIC_SERVICE_URI=" + tendermintServer + " -e TARGET_BLOCK_CREATION_TIME=10000 -v chain:/home/hotmoka/chain -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint hotmoka/tendermint-node:" + HOTMOKA_VERSION + " config-clone");

			createCommandFile("docker_tendermint_go", "docker run -it --log-driver local --rm --name hotmoka -p 8001:8001 -p 26656:26656 -v chain:/home/hotmoka/chain -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint hotmoka/tendermint-node:" + HOTMOKA_VERSION + " go");

			createCommandFile("docker_create_keys_for_tendermint_gamete", "docker run --name temp -it hotmoka/tendermint-node:" + HOTMOKA_VERSION + " moka keys create --name gamete.pem --password; docker cp temp:/home/hotmoka/gamete.pem .; docker rm temp");
			createOutputFile("docker_create_keys_for_tendermint_gamete", "Enter value for --password (the password that will be needed later to use the key pair): moon\n" + output5);

			createCommandFile("docker_tendermint_config_new", "docker run -it --rm -e PUBLIC_KEY_GAMETE_BASE64=\"" + gametePublicKeyBase64 + "\" -e CHAIN_ID=whale -e TARGET_BLOCK_CREATION_TIME=10000 -v chain:/home/hotmoka/chain -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint hotmoka/tendermint-node:" + HOTMOKA_VERSION + " config-new");

			createCommandFile("docker_tendermint_init", "docker run -it --rm -v chain:/home/hotmoka/chain -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint hotmoka/tendermint-node:" + HOTMOKA_VERSION + " init");
		}
	}

	private static class ExperimentsWithMokamintServer extends Experiments {
		private ExperimentsWithMokamintServer(Path outputDir, Path tempDir) throws Exception {
			super(outputDir, LATEX_MOKAMINT_FILE_NAME, tempDir);
		}

		@Override
		protected void generateFiles() throws Exception {
			createOutputFile("moka_nodes_manifest_show", Moka.nodesManifestShow("--uri " + mokamintServer));

			var output1 = NodesTakamakaAddressOutputs.from(Moka.nodesTakamakaAddress("--uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			TransactionReference takamakaCode = output1.getTakamakaCode();
			report("takamakaCode", takamakaCode);
			reportShort("takamakaCode", takamakaCode);

			var output2 = NodesManifestAddressOutputs.from(Moka.nodesManifestAddress("--uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			StorageReference manifest = output2.getManifest();
			report("manifest", manifest);
			reportShort("manifest", manifest);
		
			var output3 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getGamete --receiver=" + manifest + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			StorageReference gamete = output3.getResult().get().asReference(value -> new IllegalStateException("The gamete should be a storage reference"));
			report("gamete", gamete);
			reportShort("gamete", gamete);
		
			var output4 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getGasStation --receiver=" + manifest + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			StorageReference gasStation = output4.getResult().get().asReference(value -> new IllegalStateException("The gas station should be a storage reference"));
			report("gasStation", gasStation);
			reportShort("gasStation", gasStation);
		
			var output5 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getValidators --receiver=" + manifest + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			StorageReference validators = output5.getResult().get().asReference(value -> new IllegalStateException("The validators should be a storage reference"));
			report("validators", validators);
			reportShort("validators", validators);
		
			var output6 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.GAMETE_NAME + " getMaxFaucet --receiver=" + gamete + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			BigInteger maxFaucet = output6.getResult().get().asBigInteger(value -> new IllegalStateException("The max faucet threshold should be a BigInteger"));
			report("maxFaucet", maxFaucet.toString());
		
			var output7 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getChainId --receiver=" + manifest + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			String chainId = output7.getResult().get().asString(value -> new IllegalStateException("The chain identifier should be a String"));
			report("chainId", chainId);
		
			var output8 = KeysCreateOutputs.from(Moka.keysCreate("--name account1.pem --output-dir=" + tempDir + " --password=chocolate --json"));
			createCommandFile("moka_keys_create_account1", "moka keys create --name=account1.pem --password");
			createOutputFile("moka_keys_create_account1", "Enter value for --password (the password that will be needed later to use the key pair): chocolate\n" + output8);
			report("accountOnePublicKeyBaseFiftyeight", output8.getPublicKeyBase58());
			String publicKeyAccount1Base64 = output8.getPublicKeyBase64();
			report("accountOnePublicKeyBaseSixtyfour", publicKeyAccount1Base64);
			report("accountOneTendermintAddress", output8.getTendermintAddress());
			reportShort("accountOnePublicKeyBaseSixtyfour", publicKeyAccount1Base64);
		
			var account1Balance = 50000000000000L;
			report("accountOneBalance", Long.toString(account1Balance));
			var output9 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet " + account1Balance + " " + tempDir.resolve("account1.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --password=chocolate --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createCommandFile("moka_accounts_create_account1", "moka accounts create faucet " + account1Balance + " account1.pem --password --uri " + mokamintServer);
			createOutputFile("moka_accounts_create_account1", "Enter value for --password (the password of the key pair): chocolate\n" + output9);
			StorageReference account1 = output9.getAccount().get();
			report("accountOneTransaction", output9.getTransaction().toString());
			report("accountOne", account1);
			reportShort("accountOne", account1);
		
			createCommandFile("docker_run_moka_objects_show_account1", "docker run -it --rm hotmoka/mokamint-node:" + HOTMOKA_VERSION + " moka objects show " + account1 + " --uri " + mokamintServer);
			// we actually run the following locally, not inside docker...
			String mokaObjectShowAccount1Output = Moka.objectsShow(account1 + " --uri " + mokamintServer);
			createOutputFile("docker_run_moka_objects_show_account1", mokaObjectShowAccount1Output);
			createCommandFile("moka_objects_show_account1", "moka objects show " + account1 + " --uri " + mokamintServer);
			createOutputFile("moka_objects_show_account1", mokaObjectShowAccount1Output);
		
			var recharge = 200000;
			report("accountOneRecharge", String.valueOf(recharge));
			createCommandFile("moka_accounts_send_account1", "moka accounts send faucet " + recharge + " " + account1 + " --uri=" + mokamintServer);
			var output10 = Moka.accountsSend("faucet " + recharge + " " + account1 + " --uri=" + mokamintServer + " --timeout=" + TIMEOUT);
			createOutputFile("moka_accounts_send_account1", output10);
		
			var accountMokito = StorageValues.reference("826b150ccd5bf7ac6d8b07a7d3d12eba2c0eada93a91318e3b8a61397c702412#0");
			report("accountMokito", accountMokito);
			reportShort("accountMokito", accountMokito);
		
			createCommandFile("moka_accounts_import_account_mokito", "moka accounts import bench cradle hat deer game nation stage extra elite alarm pupil eight sudden amused uniform clip catch apart alpha autumn fat away theme ski excuse truly gospel clay silent stairs route pyramid exile find outside decade");
			var output11 = Moka.accountsImport("bench cradle hat deer game nation stage extra elite alarm pupil eight sudden amused uniform clip catch apart alpha autumn fat away theme ski excuse truly gospel clay silent stairs route pyramid exile find outside decade --output-dir=" + tempDir);
			createOutputFile("moka_accounts_import_account_mokito", output11);
		
			createCommandFile("moka_accounts_export_account1", "moka account export " + account1);
			createOutputFile("moka_accounts_export_account1", Moka.accountsExport(account1 + " --dir=" + tempDir));
		
			createCommandFile("moka_keys_create_anonymous", "moka keys create --name=anonymous.pem --password");
			var output12 = KeysCreateOutputs.from(Moka.keysCreate("--name anonymous.pem --output-dir=" + tempDir + " --password=kiwis --json"));
			String anonymousPublicKeyBase58 = output12.getPublicKeyBase58();
			createOutputFile("moka_keys_create_anonymous", "Enter value for --password (the password that will be needed later to use the key pair): kiwis\n" + output12);
			report("publicKeyBaseFifthyeightAnonymous", anonymousPublicKeyBase58);
		
			var amountSendToAnonymous = 10000;
			report("sentToAnonymous", String.valueOf(amountSendToAnonymous));
			createCommandFile("moka_accounts_send_account1_anonymous", "moka accounts send " + account1 + " " + amountSendToAnonymous + " " + anonymousPublicKeyBase58 + " --password-of-sender --uri=" + mokamintServer);
			var output13 = AccountsSendOutputs.from(Moka.accountsSend(account1 + " " + amountSendToAnonymous + " " + anonymousPublicKeyBase58 + " --password-of-sender=chocolate --dir=" + tempDir + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_accounts_send_account1_anonymous", "Enter value for --password-of-sender (the password of the sender): chocolate\n" + output13);
			report("accountAnonymous", output13.getDestinationInAccountsLedger().get());
		
			createCommandFile("moka_keys_bind_anonymous", "moka keys bind anonymous.pem --password --uri=" + mokamintServer);
			// we use a wrong password to simulate the fact that the key has not been bound yet
			createOutputFile("moka_keys_bind_anonymous_not_yet", Moka.keysBind(tempDir.resolve("anonymous.pem") + " --password=wrong --uri=" + mokamintServer + " --output-dir=" + tempDir + " --timeout=" + TIMEOUT));
			var output14 = Moka.keysBind(tempDir.resolve("anonymous.pem") + " --password=kiwis --uri=" + mokamintServer + " --output-dir=" + tempDir + " --timeout=" + TIMEOUT);
			createOutputFile("moka_keys_bind_anonymous", "Enter value for --password (the password of the key pair): kiwis\n" + output14);
			
			createCommandFile("moka_jars_install", "moka jars install " + account1 + " io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar --yes --password-of-payer --uri=" + mokamintServer);
			Path jar = HOME.resolve(".m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar");
			var output15 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar + " --password-of-payer=chocolate --dir=" + tempDir + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_jars_install", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output15);
			TransactionReference familyAddress = output15.getJar().get();
			report("familyAddress", familyAddress);
			reportShort("familyAddress", familyAddress);
		
			createCommandFile("mvn_exec_family_1", "mvn compile exec:java -Dexec.mainClass=\"io.hotmoka.tutorial.examples.runs.Family\" -Dexec.args=\""
					+ mokamintServer + " " + HOTMOKA_TUTORIAL_DIR + " " + account1 + " chocolate\"");
			String runFamilyMain = run(() -> Family.main(new String[] { mokamintServer.toString(), tempDir.toString(), account1.toString(), "chocolate" }));
			createOutputFile("mvn_exec_family_1", runFamilyMain);
			int start = "jar installed at ".length();
			var codeFamilyAddress = TransactionReferences.of(runFamilyMain.substring(start, start + 64));
			report("codeFamilyAddress", codeFamilyAddress);
			reportShort("codeFamilyAddress", codeFamilyAddress);
		
			createCommandFile("moka_objects_create_person_failed", "moka objects create " + account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + familyAddress + " --password-of-payer --uri=" + mokamintServer);
			var output16 = Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + familyAddress + " --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=chocolate");
			createOutputFile("moka_objects_create_person_failed", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\nDo you really want to call constructor\n  public ...Person(java.lang.String,int,int,int,...Person,...Person)\n  spending up to 1000000 gas units at the price of 1 pana per unit (that is, up to 1000000 panas) [Y/N] Y\n" + output16);
		
			createCommandFile("moka_jars_install_2", "cd io-takamaka-code-examples-family\nmvn clean install\ncd ..\nmoka jars install " + account1 + " io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar --yes --password-of-payer --uri=" + mokamintServer);
			Path jar2 = HOME.resolve(".m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_storage/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_storage-" + HOTMOKA_VERSION + ".jar");
			var output17 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar2 + " --password-of-payer=chocolate --dir=" + tempDir + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_jars_install_2", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output17);
			TransactionReference family2Address = output17.getJar().get();
			report("familyTwoAddress", family2Address);
			reportShort("familyTwoAddress", family2Address);
		
			createCommandFile("moka_objects_create_person", "moka objects create " + account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + family2Address + " --password-of-payer --uri=" + mokamintServer);
			var output18 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + family2Address + " --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --json --password-of-payer=chocolate"));
			createOutputFile("moka_objects_create_person", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\nDo you really want to call constructor\n  public ...Person(java.lang.String,int,int,int,...Person,...Person)\n  spending up to 1000000 gas units at the price of 1 pana per unit (that is, up to 1000000 panas) [Y/N] Y\n" + output18);
			StorageReference person = output18.getObject().get();
			report("personObject", person);
			reportShort("personObject", person);
		
			createCommandFile("moka_objects_show_person", "moka objects show " + person + " --uri " + mokamintServer);
			createOutputFile("moka_objects_show_person", Moka.objectsShow(person + " --uri " + mokamintServer));
		
			createCommandFile("mvn_exec_family_storage", "mvn compile exec:java -Dexec.mainClass=\"io.hotmoka.tutorial.examples.runs.FamilyStorage\" -Dexec.args=\""
					+ mokamintServer + " " + HOTMOKA_TUTORIAL_DIR + " " + account1 + " chocolate\"");
			String runFamilyStorageMain = run(() -> FamilyStorage.main(new String[] { mokamintServer.toString(), tempDir.toString(), account1.toString(), "chocolate" }));
			createOutputFile("mvn_exec_family_storage", runFamilyStorageMain);
			start = "new object allocated at ".length();
			var person2Object = StorageValues.reference(runFamilyStorageMain.substring(start, start + 66));
			report("personObjectTwo", person2Object);
			reportShort("personObjectTwo", person2Object);
			
			createCommandFile("moka_objects_call_toString", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.family.Person toString --uri=" + mokamintServer + " --password-of-payer --receiver=" + person2Object);
			var output19 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.family.Person toString --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=chocolate --receiver=" + person2Object);
			createOutputFile("moka_objects_call_toString", output19);
		
			createCommandFile("moka_jars_install_3", "cd io-takamaka-code-examples-family\nmvn clean install\ncd ..\nmoka jars install " + account1 + " io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar --yes --password-of-payer --uri=" + mokamintServer);
			Path jar3 = HOME.resolve(".m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_exported/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_exported-" + HOTMOKA_VERSION + ".jar");
			var output20 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar3 + " --password-of-payer=chocolate --dir=" + tempDir + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_jars_install_3", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output20);
			TransactionReference familyExportedAddress = output20.getJar().get();
			report("familyExportedAddress", familyExportedAddress);
			reportShort("familyExportedAddress", familyExportedAddress);

			createCommandFile("moka_objects_create_person_exported", "moka objects create " + account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + familyExportedAddress + " --password-of-payer --uri=" + mokamintServer);
			var output21 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + familyExportedAddress + " --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --json --password-of-payer=chocolate"));
			createOutputFile("moka_objects_create_person_exported", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\nDo you really want to call constructor\n  public ...Person(java.lang.String,int,int,int,...Person,...Person)\n  spending up to 1000000 gas units at the price of 1 pana per unit (that is, up to 1000000 panas) [Y/N] Y\n" + output21);
			StorageReference personExported = output21.getObject().get();
			report("personExportedObject", personExported);
			reportShort("personExportedObject", personExported);
		
			createCommandFile("moka_objects_call_toString_exported", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.family.Person toString --uri=" + mokamintServer + " --password-of-payer --receiver=" + personExported);
			var output22 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.family.Person toString --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=chocolate --receiver=" + personExported);
			createOutputFile("moka_objects_call_toString_exported", output22);
		
			createCommandFile("mvn_exec_family_exported", "mvn compile exec:java -Dexec.mainClass=\"io.hotmoka.tutorial.examples.runs.FamilyExported\" -Dexec.args=\""
					+ mokamintServer + " " + HOTMOKA_TUTORIAL_DIR + " " + account1 + " chocolate\"");
			String runFamilyExportedMain = run(() -> FamilyExported.main(new String[] { mokamintServer.toString(), tempDir.toString(), account1.toString(), "chocolate" }));
			createOutputFile("mvn_exec_family_exported", runFamilyExportedMain);
		
			createCommandFile("moka_jars_install_gradual_ponzi", "cd io-takamaka-code-examples-ponzi\nmvn clean install\ncd ..\nmoka jars install " + account1 + " io-hotmoka-tutorial-examples-ponzi/target/io-hotmoka-tutorial-examples-ponzi-" + HOTMOKA_VERSION + ".jar --yes --password-of-payer --uri=" + mokamintServer);
			Path jar4 = HOME.resolve(".m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-ponzi_gradual/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-ponzi_gradual-" + HOTMOKA_VERSION + ".jar");
			var output23 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar4 + " --password-of-payer=chocolate --dir=" + tempDir + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_jars_install_gradual_ponzi", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output23);
			TransactionReference gradualPonziAddress = output23.getJar().get();
			report("gradualPonziAddress", gradualPonziAddress);
			reportShort("gradualPonziAddress", gradualPonziAddress);
		
			var output24 = Moka.keysCreate("--name account2.pem --output-dir=" + tempDir + " --password=orange");
			createCommandFile("moka_keys_create_account2", "moka keys create --name=account2.pem --password");
			createOutputFile("moka_keys_create_account2", "Enter value for --password (the password that will be needed later to use the key pair): orange\n" + output24);
		
			var output25 = Moka.keysCreate("--name account3.pem --output-dir=" + tempDir + " --password=apple");
			createCommandFile("moka_keys_create_account3", "moka keys create --name=account3.pem --password");
			createOutputFile("moka_keys_create_account3", "Enter value for --password (the password that will be needed later to use the key pair): apple\n" + output25);
		
			var output26 = AccountsCreateOutputs.from(Moka.accountsCreate(account1 + " 50000000000 " + tempDir.resolve("account2.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --password=orange --password-of-payer=chocolate --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createCommandFile("moka_accounts_create_account2", "moka accounts create " + account1 + " 50000000000 account2.pem --password --password-of-payer --uri " + mokamintServer);
			createOutputFile("moka_accounts_create_account2", "Enter value for --password (the password of the key pair): orange\nEnter value for --password-of-payer (the password of the payer): chocolate\nDo you really want to create the new account spending up to 200000 gas units\n"
					+ "  at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y\n" + output26);
			StorageReference account2 = output26.getAccount().get();
			report("accountTwo", account2);
			reportShort("accountTwo", account2);
		
			var output27 = AccountsCreateOutputs.from(Moka.accountsCreate(account1 + " 10000000 " + tempDir.resolve("account3.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --password=apple --password-of-payer=chocolate --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createCommandFile("moka_accounts_create_account3", "moka accounts create " + account1 + " 10000000 account3.pem --password --password-of-payer --uri " + mokamintServer);
			createOutputFile("moka_accounts_create_account3", "Enter value for --password (the password of the key pair): apple\nEnter value for --password-of-payer (the password of the payer): chocolate\nDo you really want to create the new account spending up to 200000 gas units\n"
					+ "  at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y\n" + output27);
			StorageReference account3 = output27.getAccount().get();
			report("accountThree", account3);
			reportShort("accountThree", account3);
		
			createCommandFile("moka_objects_create_gradual_ponzi", "moka objects create " + account1 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi --classpath=" + gradualPonziAddress + " --password-of-payer --uri=" + mokamintServer);
			var output28 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi --classpath=" + gradualPonziAddress + " --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --json --password-of-payer=chocolate"));
			createOutputFile("moka_objects_create_gradual_ponzi", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\nDo you really want to call constructor\n  public ...GradualPonzi()\n  spending up to 1000000 gas units at the price of 1 pana per unit (that is, up to 1000000 panas) [Y/N] Y\n" + output28);
			StorageReference gradualPonziObject = output28.getObject().get();
			report("gradualPonziObject", gradualPonziObject);
			reportShort("gradualPonziObject", gradualPonziObject);
		
			createCommandFile("moka_objects_call_invest_1", "moka objects call " + account2 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 5000 --uri=" + mokamintServer + " --password-of-payer --receiver=" + gradualPonziObject);
			var output29 = Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 5000 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=orange --receiver=" + gradualPonziObject);
			createOutputFile("moka_objects_call_invest_1", "Enter value for --password-of-payer (the password of the key pair of the payer account): orange\n" + output29);
		
			createCommandFile("moka_objects_call_invest_2", "moka objects call " + account3 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 15000 --uri=" + mokamintServer + " --password-of-payer --receiver=" + gradualPonziObject);
			var output30 = Moka.objectsCall(account3 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 15000 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=apple --receiver=" + gradualPonziObject);
			createOutputFile("moka_objects_call_invest_2", "Enter value for --password-of-payer (the password of the key pair of the payer account): apple\n" + output30);
		
			createCommandFile("moka_objects_call_invest_3", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 500 --uri=" + mokamintServer + " --password-of-payer --receiver=" + gradualPonziObject);
			var output31 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 500 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=chocolate --receiver=" + gradualPonziObject);
			createOutputFile("moka_objects_call_invest_3", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output31);
		
			var mokaObjectShowGradualPonziOutput = ObjectsShowOutputs.from(Moka.objectsShow(gradualPonziObject + " --json --uri " + mokamintServer));
			createCommandFile("moka_objects_show_gradual_ponzi", "moka objects show " + gradualPonziObject + " --uri " + mokamintServer);
			createOutputFile("moka_objects_show_gradual_ponzi", mokaObjectShowGradualPonziOutput.toString());
			var investorsField = (UpdateOfStorage) mokaObjectShowGradualPonziOutput.getFields().filter(field -> "investors".equals(field.getField().getName())).findFirst().get();
			var investorsObject = investorsField.getValue();
			var mokaObjectShowGradualPonziInvestorsOutput = Moka.objectsShow(investorsObject + " --uri " + mokamintServer);
			createCommandFile("moka_objects_show_investors", "moka objects show " + investorsObject + " --uri " + mokamintServer);
			createOutputFile("moka_objects_show_investors", mokaObjectShowGradualPonziInvestorsOutput);
		
			createCommandFile("moka_jars_install_tictactoe_revised", "cd io-takamaka-code-examples-tictactoe\nmvn clean install\ncd ..\nmoka jars install " + account1 + " io-hotmoka-tutorial-examples-tictactoe/target/io-hotmoka-tutorial-examples-tictactoe-" + HOTMOKA_VERSION + ".jar --yes --password-of-payer --uri=" + mokamintServer);
			Path jar5 = HOME.resolve(".m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-tictactoe_revised/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-tictactoe_revised-" + HOTMOKA_VERSION + ".jar");
			var output32 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar5 + " --password-of-payer=chocolate --dir=" + tempDir + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_jars_install_tictactoe_revised", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output32);
			TransactionReference ticTacToeAddress = output32.getJar().get();
			report("ticTacToeAddress", ticTacToeAddress);
			reportShort("ticTacToeAddress", ticTacToeAddress);
		
			createCommandFile("moka_objects_create_tictactoe_revised", "moka objects create " + account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe --classpath=" + ticTacToeAddress + " --password-of-payer --uri=" + mokamintServer);
			var output33 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe --classpath=" + ticTacToeAddress + " --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --json --password-of-payer=chocolate"));
			createOutputFile("moka_objects_create_tictactoe_revised", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\nDo you really want to call constructor\n  public ...TicTacToe()\n  spending up to 1000000 gas units at the price of 1 pana per unit (that is, up to 1000000 panas) [Y/N] Y\n" + output33);
			StorageReference ticTacToeObject = output33.getObject().get();
			report("ticTacToeObject", ticTacToeObject);
			reportShort("ticTacToeObject", ticTacToeObject);
		
			createCommandFile("moka_objects_call_tictactoe_play_1", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 100 1 1 --uri=" + mokamintServer + " --password-of-payer --receiver=" + ticTacToeObject);
			var output34 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 100 1 1 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=chocolate --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_play_1", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output34);
			createCommandFile("moka_objects_call_tictactoe_toString_1", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --receiver=" + ticTacToeObject);
			var output35 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_toString_1", output35);
		
			createCommandFile("moka_objects_call_tictactoe_play_2", "moka objects call " + account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 100 2 1 --uri=" + mokamintServer + " --password-of-payer --receiver=" + ticTacToeObject);
			var output36 = Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 100 2 1 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=orange --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_play_2", "Enter value for --password-of-payer (the password of the key pair of the payer account): orange\n" + output36);
			createCommandFile("moka_objects_call_tictactoe_toString_2", "moka objects call " + account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --receiver=" + ticTacToeObject);
			var output37 = Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_toString_2", output37);
		
			createCommandFile("moka_objects_call_tictactoe_play_3", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 1 2 --uri=" + mokamintServer + " --password-of-payer --receiver=" + ticTacToeObject);
			var output38 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 1 2 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=chocolate --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_play_3", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output38);
			createCommandFile("moka_objects_call_tictactoe_toString_3", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --receiver=" + ticTacToeObject);
			var output39 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_toString_3", output39);
		
			createCommandFile("moka_objects_call_tictactoe_play_4", "moka objects call " + account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 2 2 --uri=" + mokamintServer + " --password-of-payer --receiver=" + ticTacToeObject);
			var output40 = Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 2 2 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=orange --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_play_4", "Enter value for --password-of-payer (the password of the key pair of the payer account): orange\n" + output40);
			createCommandFile("moka_objects_call_tictactoe_toString_4", "moka objects call " + account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --receiver=" + ticTacToeObject);
			var output41 = Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_toString_4", output41);
		
			createCommandFile("moka_objects_call_tictactoe_play_5", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 1 3 --uri=" + mokamintServer + " --password-of-payer --receiver=" + ticTacToeObject);
			var output42 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 1 3 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=chocolate --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_play_5", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output42);
			createCommandFile("moka_objects_call_tictactoe_toString_5", "moka objects call " + account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --receiver=" + ticTacToeObject);
			var output43 = Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_toString_5", output43);
		
			createCommandFile("moka_objects_show_tictactoe", "moka objects show " + ticTacToeObject + " --uri " + mokamintServer);
			String mokaObjectsShowTicTacToeOutput = Moka.objectsShow(ticTacToeObject + " --uri " + mokamintServer);
			createOutputFile("moka_objects_show_tictactoe", mokaObjectsShowTicTacToeOutput);
		
			createCommandFile("moka_objects_call_tictactoe_play_6", "moka objects call " + account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 2 3 --uri=" + mokamintServer + " --password-of-payer --receiver=" + ticTacToeObject);
			var output44 = Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 2 3 --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --yes --password-of-payer=orange --receiver=" + ticTacToeObject);
			createOutputFile("moka_objects_call_tictactoe_play_6", "Enter value for --password-of-payer (the password of the key pair of the payer account): orange\n" + output44);

			createCommandFile("moka_jars_install_erc20", "cd io-takamaka-code-examples-erc20\nmvn clean install\ncd ..\nmoka jars install " + account1 + " io-hotmoka-tutorial-examples-erc20/target/io-hotmoka-tutorial-examples-erc20-" + HOTMOKA_VERSION + ".jar --yes --password-of-payer --uri=" + mokamintServer);
			Path jar6 = HOME.resolve(".m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-erc20/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-erc20-" + HOTMOKA_VERSION + ".jar");
			var output45 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar6 + " --password-of-payer=chocolate --dir=" + tempDir + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_jars_install_erc20", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output45);
			TransactionReference erc20Address = output45.getJar().get();
			report("ercTwentyAddress", erc20Address);
			reportShort("ercTwentyAddress", erc20Address);

			createCommandFile("moka_objects_create_erc20", "moka objects create " + account1 + " io.hotmoka.tutorial.examples.erc20.CryptoBuddy --classpath=" + erc20Address + " --yes --password-of-payer --uri=" + mokamintServer);
			var output46 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.erc20.CryptoBuddy --classpath=" + erc20Address + " --uri=" + mokamintServer + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --json --password-of-payer=chocolate"));
			createOutputFile("moka_objects_create_erc20", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output46);
			StorageReference erc20Object = output46.getObject().get();
			report("ercTwentyObject", erc20Object);
			reportShort("ercTwentyObject", erc20Object);
		}
	}

	/**
	 * Performs the experiments with the quantum-resistant keys.
	 */
	private static class ExperimentsQuantumWithMokamintServer extends Experiments {
		private ExperimentsQuantumWithMokamintServer(Path outputDir, Path tempDir) throws Exception {
			super(outputDir, LATEX_MOKAMINT_QUANTUM_FILE_NAME, tempDir);
		}

		@Override
		protected void generateFiles() throws Exception {
			var output1 = Moka.keysCreate("--name account7.pem --output-dir=" + tempDir + " --password=orange");
			createCommandFile("moka_keys_create_account7", "moka keys create --name=account7.pem --password");
			createOutputFile("moka_keys_create_account7", "Enter value for --password (the password that will be needed later to use the key pair): orange\n" + output1);
		
			var output2 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 1000000000000 " + tempDir.resolve("account7.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --password=orange --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createCommandFile("moka_accounts_create_account7", "moka accounts create faucet 1000000000000 account7.pem --password --yes --uri " + mokamintServer);
			createOutputFile("moka_accounts_create_account7", "Enter value for --password (the password of the key pair): orange\n" + output2);
			StorageReference account7 = output2.getAccount().get();
			report("accountSeven", account7);
			reportShort("accountSeven", account7);

			createCommandFile("moka_objects_show_account7", "moka objects show " + account7 + " --uri " + mokamintServer);
			createOutputFile("moka_objects_show_account7", Moka.objectsShow(account7 + " --uri " + mokamintServer));

			var output3 = Moka.keysCreate("--name account8.pem --output-dir=" + tempDir + " --password=play --signature=sha256dsa");
			createCommandFile("moka_keys_create_account8", "moka keys create --name=account8.pem --password --signature=sha256dsa");
			createOutputFile("moka_keys_create_account8", "Enter value for --password (the password that will be needed later to use the key pair): play\n" + output3);
		
			var output4 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 1000000000000 " + tempDir.resolve("account8.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --signature=sha256dsa --password=play --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createCommandFile("moka_accounts_create_account8", "moka accounts create faucet 1000000000000 account8.pem --password --signature=sha256dsa --yes --uri " + mokamintServer);
			createOutputFile("moka_accounts_create_account8", "Enter value for --password (the password of the key pair): play\n" + output4);
			StorageReference account8 = output4.getAccount().get();
			report("accountEight", account8);
			reportShort("accountEight", account8);

			createCommandFile("moka_objects_show_account8", "moka objects show " + account8 + " --uri " + mokamintServer);
			createOutputFile("moka_objects_show_account8", Moka.objectsShow(account8 + " --uri " + mokamintServer));

			var output5 = Moka.keysCreate("--name account9.pem --output-dir=" + tempDir + " --password=quantum1 --signature=qtesla1");
			createCommandFile("moka_keys_create_account9", "moka keys create --name=account9.pem --password --signature=quantum1");
			createOutputFile("moka_keys_create_account9", "Enter value for --password (the password that will be needed later to use the key pair): quantum1\n" + output5);
		
			var output6 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 1000000000000 " + tempDir.resolve("account9.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --signature=qtesla1 --password=quantum1 --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createCommandFile("moka_accounts_create_account9", "moka accounts create faucet 1000000000000 account9.pem --password --signature=qtesla1 --yes --uri " + mokamintServer);
			createOutputFile("moka_accounts_create_account9", "Enter value for --password (the password of the key pair): quantum1\n" + output6);
			StorageReference account9 = output6.getAccount().get();
			report("accountNine", account9);
			reportShort("accountNine", account9);

			var output7 = Moka.keysCreate("--name account10.pem --output-dir=" + tempDir + " --password=quantum3 --signature=qtesla3");
			createCommandFile("moka_keys_create_account10", "moka keys create --name=account10.pem --password --signature=quantum3");
			createOutputFile("moka_keys_create_account10", "Enter value for --password (the password that will be needed later to use the key pair): quantum3\n" + output7);
		
			var output8 = AccountsCreateOutputs.from(Moka.accountsCreate(account9 + " 100000 " + tempDir.resolve("account10.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --signature=qtesla3 --password=quantum3 --password-of-payer=quantum1 --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
			createCommandFile("moka_accounts_create_account10", "moka accounts create " + account9 + " 100000 account10.pem --password --password-of-payer --signature=qtesla3 --yes --uri " + mokamintServer);
			createOutputFile("moka_accounts_create_account10", "Enter value for --password (the password of the key pair): quantum3\nEnter value for --password-of-payer (the password of the payer): quantum1\n" + output8);
			StorageReference account10 = output8.getAccount().get();
			report("accountTen", account10);
			reportShort("accountTen", account10);

			createCommandFile("moka_jars_install_family_quantum", "moka jars install " + account9 + " io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar --yes --password-of-payer --uri=" + mokamintServer);
			Path jar = HOME.resolve(".m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_exported/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_exported-" + HOTMOKA_VERSION + ".jar");

			JarsInstallOutput output9;
			do {
				System.out.println("I wait one minute since the previous command might have increased the gas cost too much");
				Thread.sleep(60_000L);
				output9 = JarsInstallOutputs.from(Moka.jarsInstall(account9 + " " + jar + " --password-of-payer=quantum1 --dir=" + tempDir + " --uri=" + mokamintServer + " --json --timeout=" + TIMEOUT));
				if (output9.getJar().isEmpty())
					System.out.println(output9);
			}
			while (output9.getJar().isEmpty());

			createOutputFile("moka_jars_install_family_quantum", "Enter value for --password-of-payer (the password of the key pair of the payer account): quantum1\n" + output9);
			TransactionReference familyAddress = output9.getJar().get();
			report("familyQuantumAddress", familyAddress);
			reportShort("familyQuantumAddress", familyAddress);
		}
	}

	private static class ExperimentsWithTendermintServer extends Experiments {
		private ExperimentsWithTendermintServer(Path outputDir, Path tempDir) throws Exception {
			super(outputDir, LATEX_TENDERMINT_FILE_NAME, tempDir);
		}

		@Override
		protected void generateFiles() throws Exception {
			createCommandFile("moka_keys_create_account4", "moka keys create --name=account4.pem --password");
			String output = Moka.keysCreate("--name account4.pem --output-dir=" + tempDir + " --password=banana");
			createOutputFile("moka_keys_create_account4", "Enter value for --password (the password that will be needed later to use the key pair): banana\n" + output);

			createCommandFile("moka_keys_create_account5", "moka keys create --name=account5.pem --password");
			output = Moka.keysCreate("--name account5.pem --output-dir=" + tempDir + " --password=mango");
			createOutputFile("moka_keys_create_account5", "Enter value for --password (the password that will be needed later to use the key pair): mango\n" + output);

			createCommandFile("moka_keys_create_account6", "moka keys create --name=account6.pem --password");
			output = Moka.keysCreate("--name account6.pem --output-dir=" + tempDir + " --password=strawberry");
			createOutputFile("moka_keys_create_account6", "Enter value for --password (the password that will be needed later to use the key pair): strawberry\n" + output);

			var balance = BigInteger.valueOf(50000000000000L);

			createCommandFile("moka_accounts_create_account4", "moka accounts create faucet " + balance + " account4.pem --password --uri " + tendermintServer);
			var output4 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet " + balance + " " + tempDir.resolve("account4.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --password=banana --uri=" + tendermintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_accounts_create_account4", "Enter value for --password (the password of the key pair): banana\n" + output4);
			StorageReference account4 = output4.getAccount().get();
			report("accountFour", account4);
			reportShort("accountFour", account4);

			createCommandFile("moka_accounts_create_account5", "moka accounts create faucet " + balance + " account5.pem --password --uri " + tendermintServer);
			var output5 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet " + balance + " " + tempDir.resolve("account5.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --password=mango --uri=" + tendermintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_accounts_create_account5", "Enter value for --password (the password of the key pair): mango\n" + output5);
			StorageReference account5 = output5.getAccount().get();
			report("accountFive", account5);
			reportShort("accountFive", account5);

			createCommandFile("moka_accounts_create_account6", "moka accounts create faucet " + balance + " account6.pem --password --uri " + tendermintServer);
			var output6 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet " + balance + " " + tempDir.resolve("account6.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --password=strawberry --uri=" + tendermintServer + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_accounts_create_account6", "Enter value for --password (the password of the key pair): strawberry\n" + output6);
			StorageReference account6 = output6.getAccount().get();
			report("accountSix", account6);
			reportShort("accountSix", account6);

			System.out.println("running Auction: it will take around ten minutes");
			createCommandFile("mvn_exec_blind_auction", "mvn compile exec:java -Dexec.mainClass=\"io.hotmoka.tutorial.examples.runs.Auction\" -Dexec.args=\""
					+ tendermintServer + " " + HOTMOKA_TUTORIAL_DIR + " " + account4 + " banana " + account5 + " mango " + account6 + " strawberry\"");
			String runAuctionMain = run(() -> Auction.main(new String[] { tendermintServer.toString(), tempDir.toString(), account4.toString(), "banana", account5.toString(), "mango", account6.toString(), "strawberry" }));
			createOutputFile("mvn_exec_blind_auction", runAuctionMain);
			System.out.println("running Events: it will take around ten minutes");
			createCommandFile("mvn_exec_blind_auction_events", "mvn compile exec:java -Dexec.mainClass=\"io.hotmoka.tutorial.examples.runs.Events\" -Dexec.args=\""
					+ tendermintServer + " " + HOTMOKA_TUTORIAL_DIR + " " + account4 + " banana " + account5 + " mango " + account6 + " strawberry\"");
			String runEventsMain = run(() -> Events.main(new String[] { tendermintServer.toString(), tempDir.toString(), account4.toString(), "banana", account5.toString(), "mango", account6.toString(), "strawberry" }));
			createOutputFile("mvn_exec_blind_auction_events", runEventsMain);
		}
	}

	private abstract static class Experiments {
		private final PrintWriter writer;

		private final Path outputDir;

		/**
		 * The working directory where key pairs can be temporarily saved, for instance.
		 */
		protected final Path tempDir;

		protected Experiments(Path outputDir, String latexFilename, Path tempDir) throws Exception {
			Path outputPath = outputDir.resolve(latexFilename);

			try (var writer = new PrintWriter(outputPath.toFile())) {
				this.writer = writer;
				this.outputDir = outputDir;
				this.tempDir = tempDir;
				String message = "Generating file " + outputPath;
				System.out.println("*".repeat(message.length()));
				System.out.println(message);
				System.out.println("*".repeat(message.length()));
				generateFiles();
			}
		}

		protected abstract void generateFiles() throws Exception;

		protected void report(String line) {
			writer.println(line);
			System.out.println(line);
		}

		protected void report(String command, String implementation) {
			report("\\newcommand{\\" + command + "}{" + implementation + "}");
		}

		protected void report(String command, StorageReference reference) {
			report(command, reference.toString().replace("#", "\\#"));
		}

		protected void reportShort(String command, StorageReference reference) {
			report(command + "Short", reference.getTransaction().toString().substring(0, 16) + "\\ldots\\#" + reference.getProgressive());
		}
	
		protected void report(String command, TransactionReference reference) {
			report(command, reference.toString());
		}

		protected void reportShort(String command, TransactionReference reference) {
			reportShort(command, reference.toString());
		}

		protected void reportShort(String command, String s) {
			report(command + "Short", s.substring(0, 16) + "\\ldots");
		}

		protected void createCommandFile(String filename, String content) throws FileNotFoundException {
			Path outputFilePath = outputDir.resolve(filename + "_command.tex");
			try (PrintWriter writer = new PrintWriter(outputFilePath.toFile())) {
				writer.write("\\begin{shellcommandbox}\\begin{ttlst}\n");
				// we erase the temp directory, since the tutorial assumes that temporary
				// files get saved in the current directory
				writer.write(content.replace(tempDir + "/", ""));
				writer.write("\n\\end{ttlst}\\end{shellcommandbox}\n");
				System.out.println("Created " + outputFilePath);
			}
		}
		
		protected void createOutputFile(String filename, String content) throws FileNotFoundException {
			Path outputFilePath = outputDir.resolve(filename + "_output.tex");
			try (PrintWriter writer = new PrintWriter(outputFilePath.toFile())) {
				writer.write("\\begin{shellbox}\\begin{ttlst}\n");
				// we erase the temp directory, since the tutorial assumes that temporary
				// files get saved in the current directory
				writer.write(content.replace(tempDir + "/", ""));
				writer.write("\\end{ttlst}\\end{shellbox}\n");
				System.out.println("Created " + outputFilePath);
			}
		}

		private interface Command {
			void run() throws Exception;
		}

		/**
		 * Runs the given command, inside a sand-box where the
		 * standard output is redirected into the resulting string.
		 * 
		 * @param command the command to run
		 * @return what the command has written into the standard output
		 * @throws Exception if the command or the construction of the return value failed
		 */
		protected String run(Command command) throws Exception {
			var originalOut = System.out;
			var originalErr = System.err;
		
			try (var baos = new ByteArrayOutputStream(); var out = new PrintStream(baos)) {
				System.setOut(out);
				System.setErr(out);
				command.run();
				return new String(baos.toByteArray());
			}
			finally {
				System.setOut(originalOut);
				System.setErr(originalErr);
			}
		}

		protected String shell(String cmdline) throws IOException, InterruptedException, TimeoutException {
			var process = new ProcessBuilder(new String[] {"bash", "-c", cmdline})
					.redirectErrorStream(true)
					.directory(tempDir.toFile())
					.start();

			var output = "";
			var br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ( (line = br.readLine()) != null )
				output += line + "\n";

			//There should really be a timeout here.
			if (!process.waitFor(120, TimeUnit.SECONDS))
				throw new TimeoutException();

			return output;
		}
	}

	/**
	 * Loads the Java logging configuration from the resources. If the property
	 * {@code java.util.logging.config.file} is defined, nothing will be loaded.
	 */
	static {
		String current = System.getProperty("java.util.logging.config.file");
		if (current == null) {
			// if the property is not set, we provide a default (if it exists)
			try (var is = UpdateForNewNode.class.getModule().getResourceAsStream("logging.properties")) {
				LogManager.getLogManager().readConfiguration(is);
			}
			catch (SecurityException | IOException e) {
				throw new RuntimeException("Cannot load the logging properties file", e);
			}
		}
	}
}