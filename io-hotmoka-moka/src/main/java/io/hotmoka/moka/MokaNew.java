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
import io.hotmoka.moka.MokaNew.POMVersionProvider;
import io.hotmoka.moka.internal.Accounts;
import io.hotmoka.moka.internal.Jars;
import io.hotmoka.moka.internal.Keys;
import io.hotmoka.moka.internal.Nodes;
import io.hotmoka.moka.internal.Objects;
import picocli.CommandLine.Command;

/**
 * A command-line interface for Hotmoka.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.moka/io.hotmoka.moka.Moka
 */
@Command(
	name = "mokanew",
	header = "This is the command-line interface of Hotmoka.",
	footer = "Copyright (c) 2025 Fausto Spoto (fausto.spoto@hotmoka.io)",
	versionProvider = POMVersionProvider.class,
	subcommands = {
		Accounts.class,
		Jars.class,
		Keys.class,
		Nodes.class,
		Objects.class
	}
)
public class MokaNew extends AbstractCLI {

	private MokaNew() {}

	/**
	 * Entry point from the shell. At its end, the status message
	 * returned from the tool is returned to the shell.
	 * 
	 * @param args the command-line arguments provided to this tool
	 */
	public static void main(String[] args) {
		System.exit(main(MokaNew::new, args));
	}

	/**
	 * Runs the {@code accounts create} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsCreate(String args) throws IOException {
		return run("accounts create " + args);
	}

	/**
	 * Runs the {@code accounts show} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String accountsShow(String args) throws IOException {
		return run("accounts show " + args);
	}

	/**
	 * Runs the {@code keys create} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysCreate(String args) throws IOException {
		return run("keys create " + args);
	}

	/**
	 * Runs the {@code keys show} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysShow(String args) throws IOException {
		return run("keys show " + args);
	}

	/**
	 * Runs the {@code keys export} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysExport(String args) throws IOException {
		return run("keys export " + args);
	}

	/**
	 * Runs the {@code keys import} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String keysImport(String args) throws IOException {
		return run("keys import " + args);
	}

	/**
	 * Runs the {@code jars verify} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String jarsVerify(String args) throws IOException {
		return run("jars verify " + args);
	}

	/**
	 * Runs the {@code jars instrument} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String jarsInstrument(String args) throws IOException {
		return run("jars instrument " + args);
	}

	/**
	 * Runs the {@code nodes faucet} command with the given arguments.
	 * 
	 * @param args the arguments
	 * @return what the moka tool has written into the standard output
	 * @throws IOException if the construction of the return value failed
	 */
	public static String nodesFaucet(String args) throws IOException {
		return run("nodes faucet " + args);
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
	
		try (var baos = new ByteArrayOutputStream(); var out = new PrintStream(baos)) {
			System.setOut(out);
			main(MokaNew::new, command.split(" "));
			return new String(baos.toByteArray());
		}
		finally {
			System.setOut(originalOut);
		}
	}

	static {
		loadLoggingConfig(() -> MokaNew.class.getModule().getResourceAsStream("logging.properties"));
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
			return getVersion(() -> MokaNew.class.getModule().getResourceAsStream("maven.properties"), "hotmoka.version");
		}
	}
}