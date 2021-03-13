package io.hotmoka.tools;

import io.hotmoka.tools.internal.cli.CreateAccount;
import io.hotmoka.tools.internal.cli.Faucet;
import io.hotmoka.tools.internal.cli.Info;
import io.hotmoka.tools.internal.cli.InitTendermint;
import io.hotmoka.tools.internal.cli.RestartTendermint;
import io.hotmoka.tools.internal.cli.Send;
import io.hotmoka.tools.internal.cli.State;
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
	subcommands = { InitTendermint.class, RestartTendermint.class, Faucet.class, Info.class, CreateAccount.class, Send.class, State.class, CommandLine.HelpCommand.class }, 
	description = "This is the Hotmoka CLI",
	showDefaultValues = true)
public class CLI {

	public static void main(String[] args) {
		System.exit(new CommandLine(new CLI()).execute(args));
	}
}