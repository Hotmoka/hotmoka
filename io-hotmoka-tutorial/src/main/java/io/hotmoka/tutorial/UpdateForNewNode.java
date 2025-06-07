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

// mvn clean install; java --module-path modules/explicit_or_automatic --class-path modules/unnamed --module io.hotmoka.tutorial/io.hotmoka.tutorial.UpdateForNewNode ws://panarea.hotmoka.io:8001

package io.hotmoka.tutorial;

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
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import io.hotmoka.moka.AccountsCreateOutputs;
import io.hotmoka.moka.AccountsSendOutputs;
import io.hotmoka.moka.JarsInstallOutputs;
import io.hotmoka.moka.KeysBindOutputs;
import io.hotmoka.moka.KeysCreateOutputs;
import io.hotmoka.moka.KeysExportOutputs;
import io.hotmoka.moka.Moka;
import io.hotmoka.moka.NodesManifestAddressOutputs;
import io.hotmoka.moka.NodesTakamakaAddressOutputs;
import io.hotmoka.moka.ObjectsCallOutputs;
import io.hotmoka.moka.ObjectsCreateOutputs;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.tutorial.examples.Family;
import io.takamaka.code.constants.Constants;

/**
 * This executable runs experiments against a remote Hotmoka node and
 * reports the results inside the "replacements.sh" script, so that
 * the recompilation of the tutorial will embed the exact results of the experiments.
 */
public class UpdateForNewNode {
	public final static int TIMEOUT = 120000;

	public static void main(String[] args) throws Exception {
		String server = args.length > 0 ? args[0] : "ws://panarea.hotmoka.io:8001";

		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("src/main/bash/replacements.sh")))) {
			new Experiments(new URI(server), writer);
		}
	}

	private static class Experiments {
		private final PrintWriter writer;
		private final String hotmokaVersion;
		private final String takamakaVersion;

		/**
		 * The working directory where key pairs can be temporarily saved, for instance.
		 */
		private final Path dir;

		private Experiments(URI uri, PrintWriter writer) throws Exception {
			this.writer = writer;
			this.dir = Files.createTempDirectory("tmp");
			System.out.println(dir);

			try (var is = UpdateForNewNode.class.getModule().getResourceAsStream("maven.properties")) {
				var mavenProperties = new Properties();
				mavenProperties.load(is);
				this.hotmokaVersion = mavenProperties.getProperty("hotmoka.version");
				if (hotmokaVersion == null)
					throw new IOException("The property file does not contain a hotmoka.version property");

				report("sed -i 's/@hotmoka_version/" + hotmokaVersion + "/g' target/Tutorial.md");

				this.takamakaVersion = mavenProperties.getProperty("io.takamaka.code.version");
				if (takamakaVersion == null)
					throw new IOException("The property file does not contain a io.takamaka.code.version property");

				report("sed -i 's/@takamaka_version/" + takamakaVersion + "/g' target/Tutorial.md");
				report("sed -i 's/@takamaka_version/" + takamakaVersion + "/g' target/pics/state1.fig");
				report("sed -i 's/@takamaka_version/" + takamakaVersion + "/g' target/pics/state2.fig");
				report("sed -i 's/@takamaka_version/" + takamakaVersion + "/g' target/pics/state3.fig");
			}

			var output1 = NodesTakamakaAddressOutputs.from(Moka.nodesTakamakaAddress("--uri=" + uri + " --json --timeout=" + TIMEOUT));
			TransactionReference takamakaCode = output1.getTakamakaCode();
			report("sed -i 's/@takamakaCode/" + takamakaCode + "/g' target/Tutorial.md");

			var output2 = NodesManifestAddressOutputs.from(Moka.nodesManifestAddress("--uri=" + uri + " --json --timeout=" + TIMEOUT));
			StorageReference manifest = output2.getManifest();
			report("sed -i 's/@manifest/" + manifest + "/g' target/Tutorial.md");

			var output3 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getGamete --receiver=" + manifest + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
			StorageReference gamete = output3.getResult().get().asReference(value -> new IllegalStateException("The gamete should be a storage reference"));
			report("sed -i 's/@gamete/" + gamete + "/g' target/Tutorial.md");

			var output4 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getGasStation --receiver=" + manifest + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
			StorageReference gasStation = output4.getResult().get().asReference(value -> new IllegalStateException("The gas station should be a storage reference"));
			report("sed -i 's/@gasStation/" + gasStation + "/g' target/Tutorial.md");

			var output5 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getValidators --receiver=" + manifest + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
			StorageReference validators = output5.getResult().get().asReference(value -> new IllegalStateException("The validators should be a storage reference"));
			report("sed -i 's/@validators/" + validators + "/g' target/Tutorial.md");

			var output6 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.GAMETE_NAME + " getMaxFaucet --receiver=" + gamete + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
			BigInteger maxFaucet = output6.getResult().get().asBigInteger(value -> new IllegalStateException("The max faucet threshold should be a BigInteger"));
			report("sed -i 's/@maxFaucet/" + maxFaucet + "/g' target/Tutorial.md");

			var output7 = ObjectsCallOutputs.from(Moka.objectsCall(manifest + " " + Constants.MANIFEST_NAME + " getChainId --receiver=" + manifest + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
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

			var output9 = AccountsCreateOutputs.from(Moka.accountsCreate("faucet 50000000000 " + dir.resolve("account1.pem") + " --dir=" + dir + " --output-dir=" + dir + " --password=chocolate --uri=" + uri + " --json --timeout=" + TIMEOUT));
			StorageReference account1 = output9.getAccount().get();
			report("sed -i 's/@transaction_account1/" + output9.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account1/" + account1 + "/g' target/Tutorial.md");
			String account1String = account1.toString();
			int account1ProgressivePos = account1String.lastIndexOf('#');
			String account1Short = account1String.substring(0, 11) + "..." + account1String.substring(account1ProgressivePos);
			report("sed -i 's/@short_account1/" + account1Short + "/g' target/pics/state2.fig");
			report("sed -i 's/@short_account1/" + account1Short + "/g' target/pics/state3.fig");

			var output10 = AccountsSendOutputs.from(Moka.accountsSend("faucet 200000 " + account1 + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
			report("sed -i 's/@transaction_recharge_account1/" + output10.getTransaction() + "/g' target/Tutorial.md");

			var output11 = KeysExportOutputs.from(Moka.keysExport(account1 + " --dir=" + dir + " --json"));
			var ai = new AtomicInteger(1);
			String words = output11.getBip39Words().map(s -> String.format("%2d: %s", ai.getAndIncrement(), s)).collect(Collectors.joining("\\n"));
			report("sed -i 's/@36words_of_account1/" + words + "/g' target/Tutorial.md");

			var output12 = KeysCreateOutputs.from(Moka.keysCreate("--name anonymous.pem --output-dir=" + dir + " --password=kiwis --json"));
			String anonymousPublicKeyBase58 = output12.getPublicKeyBase58();
			report("sed -i 's/@publickeybase58anonymous/" + anonymousPublicKeyBase58 + "/g' target/Tutorial.md");
			// we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
			report("sed -i \"s/@publickeyanonymous/" + output12.getPublicKeyBase64().replace("/", "\\/") + "/g\" target/Tutorial.md");
			report("sed -i 's/@tendermintaddressanonymous/" + output12.getTendermintAddress() + "/g' target/Tutorial.md");

			var output13 = AccountsSendOutputs.from(Moka.accountsSend(account1 + " 10000 " + anonymousPublicKeyBase58 + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
			report("sed -i 's/@transactionsendanonymous/" + output13.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@account_anonymous/" + output13.getDestinationInAccountsLedger().get() + "/g' target/Tutorial.md");

			KeysBindOutputs.from(Moka.keysBind(dir.resolve("anonymous.pem") + " --password=kiwis --uri=" + uri + " --output-dir=" + dir + " --json --timeout=" + TIMEOUT));

			Path jar = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-takamaka-code-examples-family/" + takamakaVersion + "/io-takamaka-code-examples-family-" + takamakaVersion + ".jar");
			var output14 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
			report("sed -i 's/@transactioninstallfamily/" + output14.getTransaction() + "/g' target/Tutorial.md");
			TransactionReference familyAddress = output14.getJar().get();
			report("sed -i 's/@family_address/" + familyAddress + "/g' target/Tutorial.md");
			report("sed -i 's/@short_family_address/" + familyAddress.toString().substring(0, 10) + ".../g' target/pics/state3.fig");
			String result = run(() -> Family.main(new String[] { dir.toString(), account1.toString(), "chocolate" }));
			int start = "jar installed at: ".length();
			var codeFamilyAddress = TransactionReferences.of(result.substring(start, start + 64));
			report("sed -i 's/@code_family_address/" + codeFamilyAddress + "/g' target/Tutorial.md");

			var output15 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " family.Person Einstein 14 4 1879 null null --classpath=" + familyAddress + " --uri=" + uri + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate"));
			report("sed -i 's/@family_creation_transaction_failed/" + output15.getTransaction() + "/g' target/Tutorial.md");

			Path jar2 = Paths.get(System.getProperty("user.home") + "/.m2/repository/io/hotmoka/io-takamaka-code-examples-family_storage/" + takamakaVersion + "/io-takamaka-code-examples-family_storage-" + takamakaVersion + ".jar");
			var output16 = JarsInstallOutputs.from(Moka.jarsInstall(account1 + " " + jar2 + " --password-of-payer=chocolate --dir=" + dir + " --uri=" + uri + " --json --timeout=" + TIMEOUT));
			TransactionReference family2Address = output16.getJar().get();
			report("sed -i 's/@family2_address/" + family2Address + "/g' target/Tutorial.md");

			var output17 = ObjectsCreateOutputs.from(Moka.objectsCreate(account1 + " family.Person Einstein 14 4 1879 null null --classpath=" + family2Address + " --uri=" + uri + " --timeout=" + TIMEOUT + " --dir=" + dir + " --json --password-of-payer=chocolate"));
			report("sed -i 's/@family_creation_transaction_success/" + output17.getTransaction() + "/g' target/Tutorial.md");
			report("sed -i 's/@person_object/" + output17.getObject().get() + "/g' target/Tutorial.md");
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