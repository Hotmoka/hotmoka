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

import java.io.IOException;

import io.hotmoka.cli.AbstractCLI;
import io.hotmoka.cli.AbstractPropertyFileVersionProvider;
import io.hotmoka.moka.MokaNew.POMVersionProvider;
import io.hotmoka.moka.internal.Jars;
import io.hotmoka.moka.internal.Keys;
import io.hotmoka.moka.internal.Node;
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
		Jars.class,
		Keys.class,
		Node.class
	}
)
public class MokaNew extends AbstractCLI {

	private MokaNew() {}

	/**
	 * Entry point from the shell.
	 * 
	 * @param args the command-line arguments provided to this tool
	 */
	public static void main(String[] args) {
		main(MokaNew::new, args);
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