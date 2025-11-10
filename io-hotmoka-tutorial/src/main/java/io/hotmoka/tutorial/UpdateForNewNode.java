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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import io.hotmoka.crypto.cli.keys.KeysCreateOutputs;
import io.hotmoka.moka.AccountsCreateOutputs;
import io.hotmoka.moka.AccountsExportOutputs;
import io.hotmoka.moka.AccountsSendOutputs;
import io.hotmoka.moka.JarsInstallOutputs;
import io.hotmoka.moka.KeysBindOutputs;
import io.hotmoka.moka.Moka;
import io.hotmoka.moka.NodesManifestAddressOutputs;
import io.hotmoka.moka.NodesTakamakaAddressOutputs;
import io.hotmoka.moka.ObjectsCallOutputs;
import io.hotmoka.moka.ObjectsCreateOutputs;
import io.hotmoka.moka.ObjectsShowOutputs;
import io.hotmoka.moka.api.jars.JarsInstallOutput;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.tutorial.examples.runs.Auction;
import io.hotmoka.tutorial.examples.runs.Events;
import io.hotmoka.tutorial.examples.runs.Family;
import io.hotmoka.tutorial.examples.runs.FamilyExported;
import io.hotmoka.tutorial.examples.runs.FamilyStorage;
import io.takamaka.code.constants.Constants;

/**
 * This executable runs experiments against a couple of remote Hotmoka nodes (one for
 * Mokamint and one for Tendermint) and reports the results inside the "replacements.sh" script,
 * so that the recompilation of the tutorial will embed the exact results of the experiments.
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
	public final static int TIMEOUT = 250000;

	private UpdateForNewNode() {}

	/**
	 * Edit the {@code replacements.sh} file by rerunning the experiments of the Hotmoka tutorial.
	 * It allows one to specify the name of the file to edit and two further arguments,
	 * that are the URI of the remote nodes (for Mokamint and for Tendermint). The first defaults to
	 * {@code src/main/bash/replacements.sh} and the last two default to {@code ws://panarea.hotmoka.io:8001}
	 * and {@code ws://panarea.hotmoka.io:8002}.
	 * 
	 * @param args the arguments
	 * @throws Exception if the editing of the file fails for some reason
	 */
	public static void main(String[] args) throws Exception {
		String file = args.length > 0 ? args[0] : "src/main/bash/replacements.sh";
		String server1 = args.length > 1 ? args[1] : "ws://panarea.hotmoka.io:8001";
		String server2 = args.length > 2 ? args[2] : "ws://panarea.hotmoka.io:8002";

		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
			new Experiments(new URI(server1), new URI(server2), writer);
		}
	}

	private static class Experiments {
		private final PrintWriter writer;

		/**
		 * The working directory where key pairs can be temporarily saved, for instance.
		 */
		private final Path dir;

		private Experiments(URI mokamintURI, URI tendermintURI, PrintWriter writer) throws Exception {
			this.writer = writer;
			this.dir = Files.createTempDirectory("tmp");
			System.out.println(dir);

			report("sed -i 's/@hotmoka_version/" + HOTMOKA_VERSION + "/g' target/Tutorial.md");
			report("sed -i 's/@hotmoka_version/" + HOTMOKA_VERSION + "/g' target/pics/state1.fig");
			report("sed -i 's/@hotmoka_version/" + HOTMOKA_VERSION + "/g' target/pics/state2.fig");
			report("sed -i 's/@hotmoka_version/" + HOTMOKA_VERSION + "/g' target/pics/state3.fig");
			report("sed -i 's/@takamaka_version/" + TAKAMAKA_VERSION + "/g' target/Tutorial.md");
			report("sed -i 's/@takamaka_version/" + TAKAMAKA_VERSION + "/g' target/pics/state1.fig");
			report("sed -i 's/@takamaka_version/" + TAKAMAKA_VERSION + "/g' target/pics/state2.fig");
			report("sed -i 's/@takamaka_version/" + TAKAMAKA_VERSION + "/g' target/pics/state3.fig");
			

			report("sed -i 's/@server_mokamint/" + mokamintURI.toString().replace("/", "\\/") + "/g' target/Tutorial.md");
			report("sed -i 's/@server_tendermint/" + tendermintURI.toString().replace("/", "\\/") + "/g' target/Tutorial.md");

			var output1 = NodesTakamakaAddressOutputs.from(Moka.nodesTakamakaAddress("--uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			TransactionReference takamakaCode = output1.getTakamakaCode();
			report("sed -i 's/@takamakaCode/" + takamakaCode + "/g' target/Tutorial.md");

			var output2 = NodesManifestAddressOutputs.from(Moka.nodesManifestAddress("--uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference manifest = output2.getManifest();
			report("sed -i 's/@manifest/" + manifest + "/g' target/Tutorial.md");

			var output3 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getGamete --receiver=" + manifest + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference gamete = output3.getResult().get().asReference(value -> new IllegalStateException("The gamete should be a storage reference"));
			report("sed -i 's/@gamete/" + gamete + "/g' target/Tutorial.md");

			var output4 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getGasStation --receiver=" + manifest + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference gasStation = output4.getResult().get().asReference(value -> new IllegalStateException("The gas station should be a storage reference"));
			report("sed -i 's/@gasStation/" + gasStation + "/g' target/Tutorial.md");

			var output5 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getValidators --receiver=" + manifest + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference validators = output5.getResult().get().asReference(value -> new IllegalStateException("The validators should be a storage reference"));
			report("sed -i 's/@validators/" + validators + "/g' target/Tutorial.md");

			var output6 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.GAMETE_NAME + " getMaxFaucet --receiver=" + gamete + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			BigInteger maxFaucet = output6.getResult().get().asBigInteger(value -> new IllegalStateException("The max faucet threshold should be a BigInteger"));
			report("sed -i 's/@maxFaucet/" + maxFaucet + "/g' target/Tutorial.md");

			var output7 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getChainId --receiver=" + manifest + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			String chainId = output7.getResult().get().asString(value -> new IllegalStateException("The chain identifier should be a String"));
			report("sed -i 's/@chainid/" + chainId + "/g' target/Tutorial.md");
			report("sed -i 's/@chainid/" + chainId + "/g' target/pics/state1.fig");
			report("sed -i 's/@chainid/" + chainId + "/g' target/pics/state2.fig");
			report("sed -i 's/@chainid/" + chainId + "/g' target/pics/state3.fig");

			var output8 = KeysCreateOutputs.from(Moka.keysCreate("--name account1.pem --output-dir=" + dir + " --password=chocolate --json"));
			report("sed -i 's/@publickeybase58account1/" + output8.getPublicKeyBase58() + "/g' target/Tutorial.md");
			// we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
			String publicKeyAccount1Base64 = output8.getPublicKeyBase64();
			report("sed -i \"s/@publickeyaccount1/" + publicKeyAccount1Base64.replace("/", "\\/") + "/g\" target/Tutorial.md");
			report("sed -i 's/@tendermintaddressaccount1/" + output8.getTendermintAddress() + "/g' target/Tutorial.md");
			String publicKeyAccount1Base64Short = publicKeyAccount1Base64.substring(0, 10).replace("/", "\\/");
			report("sed -i \"s/@short_publickeyaccount1/" + publicKeyAccount1Base64Short + ".../g\" target/pics/state2.fig");
			report("sed -i \"s/@short_publickeyaccount1/" + publicKeyAccount1Base64Short + ".../g\" target/pics/state3.fig");

			var output9 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 50000000000000 " + dir.resolve("account1.pem") + " --dir=" + dir + " --output-dir=" + dir + " --password=chocolate --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			StorageReference account1 = output9.getAccount().get();
			report("sed -i 's/@transaction_account1/" + output9.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account1/" + account1 + "/g' target/Tutorial.md");
			String account1String = account1.toString();
			int account1ProgressivePos = account1String.lastIndexOf('#');
			String account1Short = account1String.substring(0, 11) + "..." + account1String.substring(account1ProgressivePos);
			report("sed -i 's/@short_account1/" + account1Short + "/g' target/pics/state2.fig");
			report("sed -i 's/@short_account1/" + account1Short + "/g' target/pics/state3.fig");

			var output10 = AccountsSendOutputs.from(Moka.accountsSend("faucet 200000 " + account1 + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			report("sed -i 's/@transaction_recharge_account1/" + output10.getTransaction() + "/g' target/Tutorial.md");

			var output11 = AccountsExportOutputs.from(Moka.accountsExport(account1 + " --dir=" + dir + " --json"));
			var ai = new AtomicInteger(1);
			String words = output11.getBip39Words().map(s -> String.format("%2d: %s", ai.getAndIncrement(), s)).collect(Collectors.joining("\\n"));
			report("sed -i 's/@36words_of_account1/" + words + "/g' target/Tutorial.md");

			var output12 = KeysCreateOutputs.from(Moka.keysCreate("--name anonymous.pem --output-dir=" + dir + " --password=kiwis --json"));
			String anonymousPublicKeyBase58 = output12.getPublicKeyBase58();
			report("sed -i 's/@publickeybase58anonymous/" + anonymousPublicKeyBase58 + "/g' target/Tutorial.md");
			// we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
			report("sed -i \"s/@publickeyanonymous/" + output12.getPublicKeyBase64().replace("/", "\\/") + "/g\" target/Tutorial.md");
			report("sed -i 's/@tendermintaddressanonymous/" + output12.getTendermintAddress() + "/g' target/Tutorial.md");

			var output13 = AccountsSendOutputs.from(Moka.accountsSend(account1 + " 10000 " + anonymousPublicKeyBase58 + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			report("sed -i 's/@transactionsendanonymous/" + output13.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account_anonymous/" + output13.getDestinationInAccountsLedger().get() + "/g' target/Tutorial.md");

			KeysBindOutputs.from(Moka.keysBind(dir.resolve("anonymous.pem") + " --password=kiwis --uri=" + mokamintURI + " --output-dir=" + dir + " --json --timeout=" + TIMEOUT));

			Path jar = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar");
			var output14 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			report("sed -i 's/@transactioninstallfamily/" + output14.getTransaction() + "/g' target/Tutorial.md");
			TransactionReference familyAddress = output14.getJar().get();
			report("sed -i 's/@family_address/" + familyAddress + "/g' target/Tutorial.md");
			report("sed -i 's/@short_family_address/" + familyAddress.toString().substring(0, 10) + ".../g' target/pics/state3.fig");
			String runFamilyMain = run(() -> Family.main(new String[] { mokamintURI.toString(), dir.toString(), account1.toString(), "chocolate" }));
			int start = "jar installed at ".length();
			var codeFamilyAddress = TransactionReferences.of(runFamilyMain.substring(start, start + 64));
			report("sed -i 's/@code_family_address/" + codeFamilyAddress + "/g' target/Tutorial.md");

			var output15 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + familyAddress + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate"));
			report("sed -i 's/@family_creation_transaction_failed/" + output15.getTransaction() + "/g' target/Tutorial.md");

			Path jar2 = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_storage/" + HOTMOKA_VERSION + "/io-hotmoka-tutorial-examples-family_storage-" + HOTMOKA_VERSION + ".jar");
			var output16 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar2 + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + mokamintURI + " --json --timeout=" + TIMEOUT));
			TransactionReference family2Address = output16.getJar().get();
			report("sed -i 's/@family2_address/" + family2Address + "/g' target/Tutorial.md");

			var output17 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null --classpath=" + family2Address + " --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate"));
			report("sed -i 's/@family_creation_transaction_success/" + output17.getTransaction() + "/g' target/Tutorial.md");
			StorageReference personObject = output17.getObject().get();
			report("sed -i 's/@person_object/" + personObject + "/g' target/Tutorial.md");
			String runFamilyStorageMain = run(() -> FamilyStorage.main(new String[] { mokamintURI.toString(), dir.toString(), account1.toString(), "chocolate" }));
			start = "new object allocated at ".length();
			var person2Object = StorageValues.reference(runFamilyStorageMain.substring(start, start + 66));
			report("sed -i 's/@person2_object/" + person2Object + "/g' target/Tutorial.md");
			var output18 = ObjectsCallOutputs.from(Moka.objectsCall(account1 + " io.hotmoka.tutorial.examples.family.Person toString --uri=" + mokamintURI + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate --receiver=" + personObject));
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
		}

		private void report(String line) {
			writer.println(line);
			System.out.println(line);
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
			try (var is = UpdateForNewNode.class.getModule().getResourceAsStream("logging.properties")) {
				LogManager.getLogManager().readConfiguration(is);
			}
			catch (SecurityException | IOException e) {
				throw new RuntimeException("Cannot load the logging properties file", e);
			}
		}
	}
}