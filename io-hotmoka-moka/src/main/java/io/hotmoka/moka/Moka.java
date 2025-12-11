/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.moka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import io.hotmoka.cli.AbstractCLI;
import io.hotmoka.cli.AbstractPropertyFileVersionProvider;
import io.hotmoka.constants.Constants;
import io.hotmoka.moka.Moka.POMVersionProvider;
import io.hotmoka.moka.internal.Accounts;
import io.hotmoka.moka.internal.Jars;
import io.hotmoka.moka.internal.Keys;
import io.hotmoka.moka.internal.Nodes;
import io.hotmoka.moka.internal.Objects;
import io.hotmoka.moka.internal.Transactions;
import picocli.CommandLine.Command;

/**
 * A command-line interface for Hotmoka.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.moka/io.hotmoka.moka.Moka
 */
@Command(
	name = "moka",
	header = "This is the command-line interface of Hotmoka.",
	footer = "Copyright (c) 2021 Fausto Spoto (fausto.spoto@hotmoka.io)",
	versionProvider = POMVersionProvider.class,
	subcommands = {
		Accounts.class,
		Jars.class,
		Keys.class,
		Nodes.class,
		Objects.class,
		Transactions.class
	}
)
public class Moka extends AbstractCLI {

	private Moka() {}

	/**
	 * Entry point from the shell. At its end, the status message
	 * returned from the tool is returned to the shell.
	 * 
	 * @param args the command-line arguments provided to this tool
	 */
	public static void main(String[] args) {
		System.exit(main(Moka::new, args));
	}

	/**
	 * Runs the {@code moka help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String help(String args) throws IOException {
		return run("help " + args);
	}

	/**
	 * Runs the {@code moka accounts help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsHelp(String args) throws IOException {
		return run("accounts help " + args);
	}

	/**
	 * Runs the {@code moka accounts create} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsCreate(String args) throws IOException {
		return run("accounts create " + args);
	}

	/**
	 * Runs the {@code moka accounts rotate} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsRotate(String args) throws IOException {
		return run("accounts rotate " + args);
	}

	/**
	 * Runs the {@code moka accounts send} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsSend(String args) throws IOException {
		return run("accounts send " + args);
	}

	/**
	 * Runs the {@code moka accounts show} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsShow(String args) throws IOException {
		return run("accounts show " + args);
	}

	/**
	 * Runs the {@code moka accounts export} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsExport(String args) throws IOException {
		return run("accounts export " + args);
	}

	/**
	 * Runs the {@code moka accounts import} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsImport(String args) throws IOException {
		return run("accounts import " + args);
	}

	/**
	 * Runs the {@code moka keys help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysHelp(String args) throws IOException {
		return run("keys help " + args);
	}

	/**
	 * Runs the {@code moka keys bind} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysBind(String args) throws IOException {
		return run("keys bind " + args);
	}

	/**
	 * Runs the {@code moka keys create} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysCreate(String args) throws IOException {
		return run("keys create " + args);
	}

	/**
	 * Runs the {@code moka keys show} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysShow(String args) throws IOException {
		return run("keys show " + args);
	}

	/**
	 * Runs the {@code moka keys export} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysExport(String args) throws IOException {
		return run("keys export " + args);
	}

	/**
	 * Runs the {@code moka keys import} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysImport(String args) throws IOException {
		return run("keys import " + args);
	}

	/**
	 * Runs the {@code moka jars help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String jarsHelp(String args) throws IOException {
		return run("jars help " + args);
	}

	/**
	 * Runs the {@code moka jars verify} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String jarsVerify(String args) throws IOException {
		return run("jars verify " + args);
	}

	/**
	 * Runs the {@code moka jars instrument} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String jarsInstrument(String args) throws IOException {
		return run("jars instrument " + args);
	}

	/**
	 * Runs the {@code moka jars install} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String jarsInstall(String args) throws IOException {
		return run("jars install " + args);
	}

	/**
	 * Runs the {@code moka nodes help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesHelp(String args) throws IOException {
		return run("nodes help " + args);
	}

	/**
	 * Runs the {@code moka nodes faucet} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesFaucet(String args) throws IOException {
		return run("nodes faucet " + args);
	}

	/**
	 * Runs the {@code moka nodes takamaka help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesTakamakaHelp(String args) throws IOException {
		return run("nodes takamaka help " + args);
	}

	/**
	 * Runs the {@code moka nodes takamaka address} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesTakamakaAddress(String args) throws IOException {
		return run("nodes takamaka address " + args);
	}

	/**
	 * Runs the {@code moka nodes manifest help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesManifestHelp(String args) throws IOException {
		return run("nodes manifest help " + args);
	}

	/**
	 * Runs the {@code moka nodes manifest address} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesManifestAddress(String args) throws IOException {
		return run("nodes manifest address " + args);
	}

	/**
	 * Runs the {@code moka nodes manifest show} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesManifestShow(String args) throws IOException {
		return run("nodes manifest show " + args);
	}

	/**
	 * Runs the {@code moka tendermint validators help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesTendermintValidatorsHelp(String args) throws IOException {
		return run("nodes nodes tendermint validators help " + args);
	}

	/**
	 * Runs the {@code moka nodes tendermint validators create} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesTendermintValidatorsCreate(String args) throws IOException {
		return run("nodes tendermint validators create " + args);
	}

	/**
	 * Runs the {@code moka nodes tendermint validators key} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesTendermintValidatorsKey(String args) throws IOException {
		return run("nodes tendermint validators key " + args);
	}

	/**
	 * Runs the {@code moka nodes tendermint validators buy} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesTendermintValidatorsBuy(String args) throws IOException {
		return run("nodes tendermint validators buy " + args);
	}

	/**
	 * Runs the {@code moka nodes tendermint validators sell} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesTendermintValidatorsSell(String args) throws IOException {
		return run("nodes tendermint validators sell " + args);
	}

	/**
	 * Runs the {@code moka objects help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String objectsHelp(String args) throws IOException {
		return run("objects help " + args);
	}

	/**
	 * Runs the {@code moka objects call} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String objectsCall(String args) throws IOException {
		return run("objects call " + args);
	}

	/**
	 * Runs the {@code moka objects create} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String objectsCreate(String args) throws IOException {
		return run("objects create " + args);
	}

	/**
	 * Runs the {@code moka objects show} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String objectsShow(String args) throws IOException {
		return run("objects show " + args);
	}

	/**
	 * Runs the {@code moka transactions help} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String transactionsHelp(String args) throws IOException {
		return run("transactions help " + args);
	}

	/**
	 * Runs the {@code moka transactions show} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String transactionsShow(String args) throws IOException {
		return run("transactions show " + args);
	}

	/**
	 * Runs the given command-line with the moka tool, inside a sand-box where the
	 * standard output is redirected into the resulting string. It performs as calling "moka command".
	 * 
	 * @param command the command to run with moka
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	private static String run(String command) throws IOException {
		var originalOut = System.out;
		var originalErr = System.err;
	
		try (var baos = new ByteArrayOutputStream(); var out = new PrintStream(baos)) {
			System.setOut(out);
			System.setErr(out);
			main(Moka::new, command.split(" "));
			return new String(baos.toByteArray());
		}
		finally {
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
	}

	static {
		loadLoggingConfig(() -> Moka.class.getModule().getResourceAsStream("logging.properties"));
	}

	/**
	 * A provider of the version of this tool, taken from the property
	 * declaration into the POM file.
	 */
	public static class POMVersionProvider extends AbstractPropertyFileVersionProvider {

		/**
		 * Creates the provider.
		 */
		public POMVersionProvider() {}

		@Override
		public String[] getVersion() throws IOException {
			return new String[] { Constants.HOTMOKA_VERSION };
		}
	}
}