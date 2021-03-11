package io.hotmoka.tools;

import io.hotmoka.tools.internal.cli.Faucet;
import io.hotmoka.tools.internal.cli.Init;
import io.hotmoka.tools.internal.cli.Info;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * A command-line interface for some basic commands over a Hotmoka node.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.tools/io.hotmoka.tools.CLI
 */
@Command(name = "CLI",
	subcommands = { Init.class, Faucet.class, Info.class, CommandLine.HelpCommand.class }, 
	description = "This is the Hotmoka CLI",
	showDefaultValues = true)
public class CLI {

	public static void main(String[] args) throws Exception {
		int exitCode = new CommandLine(new CLI()).execute(args); 
        System.exit(exitCode); 
	}
}