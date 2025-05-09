/*
Copyright 2021 Fausto Spoto

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
import java.net.URL;
import java.util.logging.LogManager;

import io.hotmoka.moka.internal.BuyValidation;
import io.hotmoka.moka.internal.SellValidation;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * A command-line interface for some basic commands over a Hotmoka node.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.tools/io.hotmoka.tools.Moka
 */
@Command(name = "moka",

	subcommands = { BuyValidation.class, SellValidation.class }, 

	description = "This is the command-line interface of Hotmoka.",

	showDefaultValues = true

	)
public class Moka {

	static {
		String current = System.getProperty("java.util.logging.config.file");
		if (current == null) {
			// if the property is not set, we provide a default (if it exists)
			URL resource = Moka.class.getClassLoader().getResource("logging.properties");
			if (resource != null)
				try {
					LogManager.getLogManager().readConfiguration(resource.openStream());
				}
				catch (SecurityException | IOException e) {
					throw new IllegalStateException("Cannot load logging.properties file", e);
				}
		}
	}

	/**
	 * Builds the command-line tool.
	 */
	public Moka() {}

	/**
	 * Runs the command-line tool with the options provided in the given arguments.
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		System.exit(new CommandLine(new Moka()).execute(args));
	}

	/**
	 * Runs the command-line tool with the options provided in the given arguments' line.
	 * 
	 * @param command the arguments' line
	 */
	public static void run(String command) {
		new CommandLine(new Moka()).execute(command.split(" "));
	}
}