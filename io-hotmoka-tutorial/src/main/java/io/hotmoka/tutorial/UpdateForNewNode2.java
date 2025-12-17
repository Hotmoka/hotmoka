/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.tutorial;

import static io.hotmoka.constants.Constants.HOTMOKA_VERSION;
import static io.takamaka.code.constants.Constants.TAKAMAKA_VERSION;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;

import io.hotmoka.crypto.cli.keys.KeysCreateOutputs;
import io.hotmoka.moka.AccountsCreateOutputs;
import io.hotmoka.moka.AccountsSendOutputs;
import io.hotmoka.moka.JarsInstallOutputs;
import io.hotmoka.moka.Moka;
import io.hotmoka.moka.NodesManifestAddressOutputs;
import io.hotmoka.moka.NodesTakamakaAddressOutputs;
import io.hotmoka.moka.ObjectsCallOutputs;
import io.hotmoka.moka.ObjectsCreateOutputs;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.tutorial.examples.runs.Family;
import io.hotmoka.tutorial.examples.runs.FamilyStorage;
import io.takamaka.code.constants.Constants;

/**
 * This executable runs experiments against a couple of remote Hotmoka nodes (one for
 * Mokamint and one for Tendermint) and reports the results inside the "parameters.tex" latex file,
 * so that the recompilation of the tutorial will embed the exact results of the experiments.
 * 
 * Run it with:
 * 
 * mvn exec:exec -Dexec.executable="java" -Dexec.args="-cp %classpath io.hotmoka.tutorial.UpdateForNewNode"
 */
public class UpdateForNewNode2 {

	/**
	 * The timeout (in milliseconds) used for the communication to the remote nodes. Better to use a relatively high
	 * value, since the Mokamint node has a varying block creation rate.
	 */
	public final static int TIMEOUT = 250000;

	private UpdateForNewNode2() {}

	/**
	 * Edit the {@code parameters.tex} file by rerunning the experiments of the Hotmoka tutorial.
	 * It allows one to specify the name of the directory where the files will be created and two further arguments,
	 * that are the URI of the remote nodes (for Mokamint and for Tendermint). The first defaults to
	 * {@code src/main/latex/} and the last two default to {@code ws://panarea.hotmoka.io:8001}
	 * and {@code ws://panarea.hotmoka.io:8002}.
	 * 
	 * @param args the arguments
	 * @throws Exception if the editing of the file fails for some reason
	 */
	public static void main(String[] args) throws Exception {
		Path dir = args.length > 0 ? Paths.get(args[0]) : Paths.get("src/main/latex");
		String server1 = args.length > 1 ? args[1] : "ws://panarea.hotmoka.io:8001";
		String server2 = args.length > 2 ? args[2] : "ws://panarea.hotmoka.io:8002";

		try (PrintWriter writer = new PrintWriter(dir.resolve("parameters.tex").toFile())) {
			new Experiments(new URI(server1), new URI(server2), writer, dir);
		}
	}

	private static class Experiments {
		private final PrintWriter writer;

		/**
		 * The working directory where key pairs can be temporarily saved, for instance.
		 */
		private final Path tempDir;

		/**
		 * The path where files get created.
		 */
		private final Path outputDir;

		private Experiments(URI mokamintURI, URI tendermintURI, PrintWriter writer, Path outputDir) throws Exception {
			this.writer = writer;
			this.outputDir = outputDir;
			this.tempDir = Files.createTempDirectory("tmp");
			String hotmokaRepo = "git@github.com:Hotmoka/hotmoka.git";
			System.out.println("Generating all files inside the directory " + outputDir);
			System.out.println("Saving temporary files inside the directory " + tempDir);

			createCommandFile("git_clone_hotmoka", "git clone --branch v" + HOTMOKA_VERSION + " " + hotmokaRepo);
			createCommandFile("mvn_clean_install", "mvn clean install");
			createOutputFile("moka_help", Moka.help(""));
			createCommandFile("moka_version", "moka --version");
			createOutputFile("moka_help_objects", Moka.help("objects"));
			createOutputFile("moka_objects_help_show", Moka.objectsHelp("show"));
			createOutputFile("moka_nodes_manifest_show", Moka.nodesManifestShow("--uri " + mokamintURI));
			createCommandFile("docker_run_moka", "docker run -it --rm hotmoka/mokamint-node:" + HOTMOKA_VERSION + " moka --version");
			createCommandFile("install_moka", "mkdir -p ~/Gits\ncd ~/Gits\ngit clone --branch v" + HOTMOKA_VERSION + " " + hotmokaRepo + "\ncd hotmoka\nmvn clean install -DskipTests");
			createCommandFile("export_path_moka", "export PATH=~/Gits/hotmoka/io-hotmoka-moka/src/main/bash:$PATH");
			
			report("hotmokaVersion", HOTMOKA_VERSION);
			report("takamakaVersion", TAKAMAKA_VERSION);
			report("faustoEmail", "\\email{fausto.spoto@hotmoka.io}");
			report("hotmokaRepo", hotmokaRepo);
			var hotmokaTutorialDir = "hotmoka_tutorial";
			report("hotmokaTutorialDir", hotmokaTutorialDir.replace("_", "\\_"));
			report("serverMokamint", mokamintURI.toString());
			report("serverTendermint", tendermintURI.toString());

			var output1 = NodesTakamakaAddressOutputs.from(Moka.nodesTakamakaAddress("--uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			TransactionReference takamakaCode = output1.getTakamakaCode();
			report("takamakaCode", takamakaCode);
			reportShort("takamakaCode", takamakaCode);

			var output2 = NodesManifestAddressOutputs.from(Moka.nodesManifestAddress("--uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference manifest = output2.getManifest();
			report("manifest", manifest);
			reportShort("manifest", manifest);

			var output3 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getGamete --receiver=" + manifest + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference gamete = output3.getResult().get().asReference(value -> new IllegalStateException("The gamete should be a storage reference"));
			report("gamete", gamete);
			reportShort("gamete", gamete);

			var output4 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getGasStation --receiver=" + manifest + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference gasStation = output4.getResult().get().asReference(value -> new IllegalStateException("The gas station should be a storage reference"));
			report("gasStation", gasStation);
			reportShort("gasStation", gasStation);

			var output5 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getValidators --receiver=" + manifest + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference validators = output5.getResult().get().asReference(value -> new IllegalStateException("The validators should be a storage reference"));
			report("validators", validators);
			reportShort("validators", validators);

			var output6 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.GAMETE_NAME + " getMaxFaucet --receiver=" + gamete + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			BigInteger maxFaucet = output6.getResult().get().asBigInteger(value -> new IllegalStateException("The max faucet threshold should be a BigInteger"));
			report("maxFaucet", maxFaucet.toString());

			var output7 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getChainId --receiver=" + manifest + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			String chainId = output7.getResult().get().asString(value -> new IllegalStateException("The chain identifier should be a String"));
			report("chainId", chainId);

			var output8 = KeysCreateOutputs.from(Moka.keysCreate("--name account1.pem --output-dir=" + tempDir + " --password=chocolate --json"));
			createCommandFile("moka_keys_create_account1", "moka keys create --name=account1.pem --password");
			createOutputFile("moka_keys_create_account1", "Enter value for --password (the password that will be needed later to use the key pair): chocolate\n" + output8);
			report("accountOnePublicKeyBaseFiftyeight", output8.getPublicKeyBase58());
			String publicKeyAccount1Base64 = output8.getPublicKeyBase64();
			report("accountOnePublicKeyBaseSixtyfour", publicKeyAccount1Base64);
			report("accountOneTendermintAddress", output8.getTendermintAddress());
			String publicKeyAccount1Base64Short = publicKeyAccount1Base64.substring(0, 16) + "\\ldots";
			report("accountOnePublicKeyBaseSixtyfourShort", publicKeyAccount1Base64Short);

			var account1Balance = 50000000000000L;
			report("accountOneBalance", Long.toString(account1Balance));
			var output9 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet " + account1Balance + " " + tempDir.resolve("account1.pem") + " --dir=" + tempDir + " --output-dir=" + tempDir + " --password=chocolate --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			createCommandFile("moka_accounts_create_account1", "moka accounts create faucet " + account1Balance + " account1.pem --password --uri " + mokamintURI);
			createOutputFile("moka_accounts_create_account1", "Enter value for --password (the password of the key pair): chocolate\n" + output9);
			StorageReference account1 = output9.getAccount().get();
			report("accountOneTransaction", output9.getTransaction().toString());
			report("accountOne", account1);
			reportShort("accountOne", account1);

			createCommandFile("docker_run_moka_objects_show_account1", "docker run -it --rm hotmoka/mokamint-node:" + HOTMOKA_VERSION + " moka objects show " + account1 + " --uri " + mokamintURI);
			// we actually run the following locally, not inside docker...
			String mokaObjectShowAccount1Output = Moka.objectsShow(account1 + " --uri " + mokamintURI);
			createOutputFile("docker_run_moka_objects_show_account1", mokaObjectShowAccount1Output);
			createCommandFile("moka_objects_show_account1", "moka objects show " + account1 + " --uri " + mokamintURI);
			createOutputFile("moka_objects_show_account1", mokaObjectShowAccount1Output);

			var recharge = 200000;
			report("accountOneRecharge", String.valueOf(recharge));
			createCommandFile("moka_accounts_send_account1", "moka accounts send faucet " + recharge + " " + account1 + " --uri=" + mokamintURI);
			var output10 = Moka.accountsSend("faucet " + recharge + " " + account1 + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT);
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
			createCommandFile("moka_accounts_send_account1_anonymous", "moka accounts send " + account1 + " " + amountSendToAnonymous + " " + anonymousPublicKeyBase58 + " --password-of-sender --uri=" + mokamintURI);
			var output13 = AccountsSendOutputs.from(Moka.accountsSend(account1 + " " + amountSendToAnonymous + " " + anonymousPublicKeyBase58 + " --password-of-sender=chocolate --dir=" + tempDir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_accounts_send_account1_anonymous", "Enter value for --password-of-sender (the password of the sender): chocolate\n" + output13);
			report("accountAnonymous", output13.getDestinationInAccountsLedger().get());

			createCommandFile("moka_keys_bind_anonymous", "moka keys bind anonymous.pem --password --uri=" + mokamintURI);
			// we use a wrong password to simulate the fact that the key has not been bound yet
			createOutputFile("moka_keys_bind_anonymous_not_yet", Moka.keysBind(tempDir.resolve("anonymous.pem") + " --password=wrong --uri=" + mokamintURI + " --output-dir=" + tempDir + " --timeout=" + TIMEOUT));
			var output14 = Moka.keysBind(tempDir.resolve("anonymous.pem") + " --password=kiwis --uri=" + mokamintURI + " --output-dir=" + tempDir + " --timeout=" + TIMEOUT);
			createOutputFile("moka_keys_bind_anonymous", "Enter value for --password (the password of the key pair): kiwis\n" + output14);
			
			createCommandFile("moka_jars_install", "moka jars install " + account1 + " io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar --password-of-payer --uri=" + mokamintURI);
			Path jar = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar");
			var output15 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar + " --password-of-payer=chocolate --dir=" + tempDir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_jars_install", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output15);
			TransactionReference familyAddress = output15.getJar().get();
			report("familyAddress", familyAddress);
			reportShort("familyAddress", familyAddress);

			createCommandFile("mvn_exec_family_1", "mvn compile exec:java -Dexec.mainClass=\"io.hotmoka.tutorial.examples.runs.Family\" -Dexec.args=\""
					+ mokamintURI + " " + hotmokaTutorialDir + " " + account1 + " chocolate\"");
			String runFamilyMain = run(() -> Family.main(new String[] { mokamintURI.toString(), tempDir.toString(), account1.toString(), "chocolate" }));
			createOutputFile("mvn_exec_family_1", runFamilyMain);
			int start = "jar installed at ".length();
			var codeFamilyAddress = TransactionReferences.of(runFamilyMain.substring(start, start + 64));
			report("codeFamilyAddress", codeFamilyAddress);
			reportShort("codeFamilyAddress", codeFamilyAddress);

			createCommandFile("moka_objects_create_person_failed", "moka objects create " + account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + familyAddress + " --password-of-payer --uri=" + mokamintURI);
			var output16 = Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + familyAddress + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --password-of-payer=chocolate");
			createOutputFile("moka_objects_create_person_failed", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\nDo you really want to call constructor\n  public ...Person(java.lang.String,int,int,int,...Person,...Person)\n  spending up to 1000000 gas units at the price of 1 pana per unit (that is, up to 1000000 panas) [Y/N] Y\n" + output16);

			createCommandFile("moka_jars_install_2", "cd io-takamaka-code-examples-family\nmvn clean install\ncd ..\nmoka jars install " + account1 + " io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar --password-of-payer --uri=" + mokamintURI);
			Path jar2 = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_storage/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_storage-" + HOTMOKA_VERSION + ".jar");
			var output17 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar2 + " --password-of-payer=chocolate --dir=" + tempDir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			createOutputFile("moka_jars_install_2", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\n" + output17);
			TransactionReference family2Address = output17.getJar().get();
			report("familyTwoAddress", family2Address);
			reportShort("familyTwoAddress", family2Address);

			createCommandFile("moka_objects_create_person", "moka objects create " + account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + family2Address + " --password-of-payer --uri=" + mokamintURI);
			var output18 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + family2Address + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + tempDir + " --json --password-of-payer=chocolate"));
			createOutputFile("moka_objects_create_person", "Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate\nDo you really want to call constructor\n  public ...Person(java.lang.String,int,int,int,...Person,...Person)\n  spending up to 1000000 gas units at the price of 1 pana per unit (that is, up to 1000000 panas) [Y/N] Y\n" + output18);
			StorageReference person = output18.getObject().get();
			report("personObject", person);
			reportShort("personObject", person);

			createCommandFile("moka_objects_show_person", "moka objects show " + person + " --uri " + mokamintURI);
			createOutputFile("moka_objects_show_person", Moka.objectsShow(person + " --uri " + mokamintURI));

			createCommandFile("mvn_exec_family_storage", "mvn compile exec:java -Dexec.mainClass=\"io.hotmoka.tutorial.examples.runs.Family\" -Dexec.args=\""
					+ mokamintURI + " " + hotmokaTutorialDir + " " + account1 + " chocolate\"");
			String runFamilyStorageMain = run(() -> FamilyStorage.main(new String[] { mokamintURI.toString(), tempDir.toString(), account1.toString(), "chocolate" }));
			createOutputFile("mvn_exec_family_storage", runFamilyStorageMain);
			start = "new object allocated at ".length();
			var person2Object = StorageValues.reference(runFamilyStorageMain.substring(start, start + 66));
			report("personObjectTwo", person2Object);
			reportShort("personObjectTwo", person2Object);
			
			/*
			var output19 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.family.Person toString --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + personObject));
			report("sed -i 's/@family_transaction_non_exported_failure/" + output18.getTransaction() + "/g' target/Tutorial.md");
			Path jar3 = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_exported/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_exported-" + HOTMOKA_VERSION + ".jar");
			var output19 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar3 + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			TransactionReference familyExportedAddress = output19.getJar().get();
			report("sed -i 's/@family_exported_address/" + familyExportedAddress + "/g' target/Tutorial.md");
			var output20 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + familyExportedAddress + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate"));
			report("sed -i 's/@family_exported_creation_transaction_success/" + output20.getTransaction() + "/g' target/Tutorial.md");
			StorageReference person3Object = output20.getObject().get();
			report("sed -i 's/@person3_object/" + person3Object + "/g' target/Tutorial.md");
			var output21 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.family.Person toString --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + person3Object));
			report("sed -i 's/@family_exported_call_toString_transaction_success/" + output21.getTransaction() + "/g' target/Tutorial.md");
			String runFamilyExportedMain = run(() -> FamilyExported.main(new String[] { mokamintURI.toString(), dir.toString(), account1.toString(), "chocolate" }));
			// the output contains a new line, to remove, and slashes, that must be escaped
			report("sed -i 's/@family_exported_call_toString_output/" + runFamilyExportedMain.trim().replace("/", "\\/") + "/g' target/Tutorial.md");

			Path jar4 = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-ponzi_gradual/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-ponzi_gradual-" + HOTMOKA_VERSION + ".jar");
			var output22 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar4 + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			TransactionReference gradualPonziAddress = output22.getJar().get();
			report("sed -i 's/@gradual_ponzi_address/" + gradualPonziAddress + "/g' target/Tutorial.md");
			KeysCreateOutputs.from(Moka.keysCreate("--name account2.pem --output-dir=" + dir + " --password=orange --json"));
			KeysCreateOutputs.from(Moka.keysCreate("--name account3.pem --output-dir=" + dir + " --password=apple --json"));
			var output23 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 50000000000 " + dir.resolve("account2.pem") + " --dir=" + dir + " --output-dir=" + dir + " --password=orange --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account2 = output23.getAccount().get();
			report("sed -i 's/@transaction_account2/" + output23.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account2/" + account2 + "/g' target/Tutorial.md");
			var output24 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 10000000 " + dir.resolve("account3.pem") + " --dir=" + dir + " --output-dir=" + dir + " --password=apple --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account3 = output24.getAccount().get();
			report("sed -i 's/@transaction_account3/" + output24.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account3/" + account3 + "/g' target/Tutorial.md");
			var output25 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi --classpath=" + gradualPonziAddress + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate"));
			report("sed -i 's/@transaction_creation_gradual_ponzi/" + output15.getTransaction() + "/g' target/Tutorial.md");
			StorageReference gradualPonziObject = output25.getObject().get();
			report("sed -i 's/@gradual_ponzi_object/" + gradualPonziObject + "/g' target/Tutorial.md");
			var output26 = ObjectsCallOutputs.from(Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 5000 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=orange --receiver=" + gradualPonziObject));
			report("sed -i 's/@transaction_account2_invest/" + output26.getTransaction() + "/g' target/Tutorial.md");
			var output27 = ObjectsCallOutputs.from(Moka.objectsCall(account3 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 15000 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=apple --receiver=" + gradualPonziObject));
			report("sed -i 's/@transaction_account3_invest/" + output27.getTransaction() + "/g' target/Tutorial.md");
			var output28 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.ponzi.GradualPonzi invest 500 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + gradualPonziObject));
			report("sed -i 's/@transaction_account1_invest/" + output28.getTransaction() + "/g' target/Tutorial.md");
			var output29 = ObjectsShowOutputs.from(Moka.objectsShow(gradualPonziObject + " --json --uri=" + mokamintURI + " --timeout=" + TIMEOUT));
			StorageValue gradualPonziList = output29.getFields().filter(update -> "investors".equals(update.getField().getName())).map(update -> update.getValue()).findFirst().get();
			report("sed -i 's/@gradual_ponzi_list/" + gradualPonziList + "/g' target/Tutorial.md");
			var output30 = ObjectsShowOutputs.from(Moka.objectsShow(gradualPonziList + " --json --uri=" + mokamintURI + " --timeout=" + TIMEOUT));
			StorageValue gradualPonziFirst = output30.getFields().filter(update -> "first".equals(update.getField().getName())).map(update -> update.getValue()).findFirst().get();
			report("sed -i 's/@gradual_ponzi_first/" + gradualPonziFirst + "/g' target/Tutorial.md");
			StorageValue gradualPonziLast = output30.getFields().filter(update -> "last".equals(update.getField().getName())).map(update -> update.getValue()).findFirst().get();
			report("sed -i 's/@gradual_ponzi_last/" + gradualPonziLast + "/g' target/Tutorial.md");

			Path jar5 = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-tictactoe_revised/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-tictactoe_revised-" + HOTMOKA_VERSION + ".jar");
			var output31 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar5 + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			TransactionReference ticTacToeAddress = output31.getJar().get();
			report("sed -i 's/@tictactoe_address/" + ticTacToeAddress + "/g' target/Tutorial.md");
			var output32 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe --classpath=" + ticTacToeAddress + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate"));
			report("sed -i 's/@tictactoe_creation_transaction/" + output32.getTransaction() + "/g' target/Tutorial.md");
			StorageReference ticTacToeObject = output32.getObject().get();
			report("sed -i 's/@tictactoe_object/" + ticTacToeObject + "/g' target/Tutorial.md");
			var output33 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 100 1 1 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_play1_transaction/" + output33.getTransaction() + "/g' target/Tutorial.md");
			var output34 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_toString1_transaction/" + output34.getTransaction() + "/g' target/Tutorial.md");
			StorageValue toString1Result = output34.getResult().get();
			// we replace new lines with the escape sequence \n (that must itself be escaped in Java...)
			report("sed -i 's/@tictactoe_toString1_result/" + toString1Result.toString().trim().replace("\n", "\\n") + "/g' target/Tutorial.md");
			var output35 = ObjectsCallOutputs.from(Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 100 2 1 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=orange --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_play2_transaction/" + output35.getTransaction() + "/g' target/Tutorial.md");
			var output36 = ObjectsCallOutputs.from(Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_toString2_transaction/" + output36.getTransaction() + "/g' target/Tutorial.md");
			StorageValue toString2Result = output36.getResult().get();
			report("sed -i 's/@tictactoe_toString2_result/" + toString2Result.toString().trim().replace("\n", "\\n") + "/g' target/Tutorial.md");
			var output37 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 1 2 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_play3_transaction/" + output37.getTransaction() + "/g' target/Tutorial.md");
			var output38 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_toString3_transaction/" + output38.getTransaction() + "/g' target/Tutorial.md");
			StorageValue toString3Result = output38.getResult().get();
			report("sed -i 's/@tictactoe_toString3_result/" + toString3Result.toString().trim().replace("\n", "\\n") + "/g' target/Tutorial.md");
			var output39 = ObjectsCallOutputs.from(Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 2 2 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=orange --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_play4_transaction/" + output39.getTransaction() + "/g' target/Tutorial.md");
			var output40 = ObjectsCallOutputs.from(Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_toString4_transaction/" + output40.getTransaction() + "/g' target/Tutorial.md");
			StorageValue toString4Result = output40.getResult().get();
			report("sed -i 's/@tictactoe_toString4_result/" + toString4Result.toString().trim().replace("\n", "\\n") + "/g' target/Tutorial.md");
			var output41 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 1 3 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_play5_transaction/" + output41.getTransaction() + "/g' target/Tutorial.md");
			var output42 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_toString5_transaction/" + output42.getTransaction() + "/g' target/Tutorial.md");
			StorageValue toString5Result = output42.getResult().get();
			report("sed -i 's/@tictactoe_toString5_result/" + toString5Result.toString().trim().replace("\n", "\\n") + "/g' target/Tutorial.md");
			var output43 = ObjectsCallOutputs.from(Moka.objectsCall(account2 + " io.hotmoka.tutorial.examples.tictactoe.TicTacToe play 0 2 3 --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=orange --receiver=" + ticTacToeObject));
			report("sed -i 's/@tictactoe_play6_transaction/" + output43.getTransaction() + "/g' target/Tutorial.md");

			KeysCreateOutputs.from(Moka.keysCreate("--name account4.pem --output-dir=" + dir + " --password=banana --json"));
			KeysCreateOutputs.from(Moka.keysCreate("--name account5.pem --output-dir=" + dir + " --password=mango --json"));
			KeysCreateOutputs.from(Moka.keysCreate("--name account6.pem --output-dir=" + dir + " --password=strawberry --json"));
			var output44 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 50000000000000 " + dir.resolve("account4.pem") + " --dir=" + dir + " --output-dir=" + dir + " --password=banana --uri=" + tendermintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account4 = output44.getAccount().get();
			report("sed -i 's/@transaction_account4/" + output44.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account4/" + account4 + "/g' target/Tutorial.md");
			var output45 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 50000000000000 " + dir.resolve("account5.pem") + " --dir=" + dir + " --output-dir=" + dir + " --password=mango --uri=" + tendermintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account5 = output45.getAccount().get();
			report("sed -i 's/@transaction_account5/" + output45.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account5/" + account5 + "/g' target/Tutorial.md");
			var output46 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 50000000000000 " + dir.resolve("account6.pem") + " --dir=" + dir + " --output-dir=" + dir + " --password=strawberry --uri=" + tendermintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account6 = output46.getAccount().get();
			report("sed -i 's/@transaction_account6/" + output46.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account6/" + account6 + "/g' target/Tutorial.md");

			System.out.println("running Auction: it will take around ten minutes");
			String runAuctionMain = run(() -> Auction.main(new String[] { tendermintURI.toString(), dir.toString(), account4.toString(), "banana", account5.toString(), "mango", account6.toString(), "strawberry" }));
			report("sed -i 's/@auction_main_output/" + runAuctionMain.toString().trim().replace("/", "\\/").replace("\n", "\\n") + "/g' target/Tutorial.md");
			System.out.println("running Events: it will take around ten minutes");
			String runEventsMain = run(() -> Events.main(new String[] { tendermintURI.toString(), dir.toString(), account4.toString(), "banana", account5.toString(), "mango", account6.toString(), "strawberry" }));
			// we cut long sentences at "by contract"
			report("sed -i 's/@events_main_output/" + runEventsMain.toString().trim().replace("/", "\\/").replace("by contract", "\n  by contract").replace("\n", "\\n") + "/g' target/Tutorial.md");

			Path jar6 = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-erc20/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-erc20-" + HOTMOKA_VERSION + ".jar");
			var output47 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar6 + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			report("sed -i 's/@transaction_install_erc20/" + output47.getTransaction() + "/g' target/Tutorial.md");
			TransactionReference erc20Address = output47.getJar().get();
			report("sed -i 's/@erc20_address/" + erc20Address + "/g' target/Tutorial.md");
			var output48 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.erc20.CryptoBuddy --classpath=" + erc20Address + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate"));
			report("sed -i 's/@erc20_creation_transaction/" + output48.getTransaction() + "/g' target/Tutorial.md");
			StorageReference erc20Object = output48.getObject().get();
			report("sed -i 's/@erc20_object/" + erc20Object + "/g' target/Tutorial.md");

			KeysCreateOutputs.from(Moka.keysCreate("--name account7.pem --output-dir=" + dir + " --password=game --json"));
			var output49 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 1000000000000 " + dir.resolve("account7.pem") + " --dir=" + dir + " --output-dir=" + dir + " --password=game --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account7 = output49.getAccount().get();
			report("sed -i 's/@transaction_account7/" + output49.getTransaction() + "/g' target/Tutorial.md");
			var output50 = ObjectsShowOutputs.from(Moka.objectsShow(account7 + " --json --uri=" + mokamintURI + " --timeout=" + TIMEOUT));
			StorageValue publicKeyAccount7 = output50.getFields().filter(update -> "publicKey".equals(update.getField().getName())).map(update -> update.getValue()).findFirst().get();
			// we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
			report("sed -i 's/@public_key_account7/" + publicKeyAccount7.toString().replace("/", "\\/") + "/g' target/Tutorial.md");
			KeysCreateOutputs.from(Moka.keysCreate("--name account8.pem --output-dir=" + dir + " --password=play --json --signature=sha256dsa"));
			var output51 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 1000000000000 " + dir.resolve("account8.pem") + " --signature=sha256dsa" + " --dir=" + dir + " --output-dir=" + dir + " --password=play --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account8 = output51.getAccount().get();
			report("sed -i 's/@transaction_account8/" + output51.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account8/" + account8 + "/g' target/Tutorial.md");
			var output52 = ObjectsShowOutputs.from(Moka.objectsShow(account8 + " --json --uri=" + mokamintURI + " --timeout=" + TIMEOUT));
			StorageValue publicKeyAccount8 = output52.getFields().filter(update -> "publicKey".equals(update.getField().getName())).map(update -> update.getValue()).findFirst().get();
			report("sed -i 's/@short_public_key_account8/" + publicKeyAccount8.toString().substring(0, 65).replace("/", "\\/") + ".../g' target/Tutorial.md");
			KeysCreateOutputs.from(Moka.keysCreate("--name account9.pem --output-dir=" + dir + " --password=quantum1 --json --signature=qtesla1"));
			var output53 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 1000000000000 " + dir.resolve("account9.pem") + " --signature=qtesla1" + " --dir=" + dir + " --output-dir=" + dir + " --password=quantum1 --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account9 = output53.getAccount().get();
			report("sed -i 's/@transaction_account9/" + output53.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account9/" + account9 + "/g' target/Tutorial.md");
			KeysCreateOutputs.from(Moka.keysCreate("--name account10.pem --output-dir=" + dir + " --password=quantum3 --json --signature=qtesla3"));
			var output54 = AccountsCreateOutputs.from(Moka.accountsCreate(account9 + " 100000 " + dir.resolve("account10.pem") + " --signature=qtesla3" + " --dir=" + dir + " --output-dir=" + dir + " --password=quantum3 --password-of-payer=quantum1 --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account10 = output54.getAccount().get();
			report("sed -i 's/@transaction_account10/" + output54.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account10/" + account10 + "/g' target/Tutorial.md");
			Path jar7 = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_exported/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_exported-" + HOTMOKA_VERSION + ".jar");
			JarsInstallOutput output55;

			do {
				System.out.println("I wait one minute since the previous command might have increased the gas cost too much");
				Thread.sleep(60_000L);
				System.out.println("Installing a jar with a qtesla1 account: this might be repeated if it fails because of a sudden gas change price");
				output55 = JarsInstallOutputs.from(Moka.jarsInstall(account9 + " " + jar7 + " --password-of-payer=quantum1 --dir=" + dir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			}
			while (output55.getJar().isEmpty());

			report("sed -i 's/@family3_install_transaction/" + output55.getTransaction() + "/g' target/Tutorial.md");
			TransactionReference family3Address = output55.getJar().get();
			report("sed -i 's/@family3_address/" + family3Address + "/g' target/Tutorial.md");
			*/
		}

		private void report(String line) {
			writer.println(line);
			System.out.println(line);
		}

		private void report(String command, String implementation) {
			report("\\newcommand{\\" + command + "}{{" + implementation + "}}");
		}

		private void report(String command, StorageReference reference) {
			report(command, reference.toString().replace("#", "\\#"));
		}

		private void reportShort(String command, StorageReference reference) {
			report(command + "Short", reference.getTransaction().toString().substring(0, 16) + "\\ldots\\#" + reference.getProgressive());
		}
	
		private void report(String command, TransactionReference reference) {
			report(command, reference.toString());
		}

		private void reportShort(String command, TransactionReference reference) {
			report(command + "Short", reference.toString().substring(0, 16) + "\\ldots");
		}

		private void createCommandFile(String filename, String content) throws FileNotFoundException {
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
		
		private void createOutputFile(String filename, String content) throws FileNotFoundException {
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
		private static String run(Command command) throws Exception {
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
	}

	/**
	 * Loads the Java logging configuration from the resources. If the property
	 * {@code java.util.logging.config.file} is defined, nothing will be loaded.
	 */
	static {
		String current = System.getProperty("java.util.logging.config.file");
		if (current == null) {
			// if the property is not set, we provide a default (if it exists)
			try (var is = UpdateForNewNode2.class.getModule().getResourceAsStream("logging.properties")) {
				LogManager.getLogManager().readConfiguration(is);
			}
			catch (SecurityException | IOException e) {
				throw new RuntimeException("Cannot load the logging properties file", e);
			}
		}
	}
}