package io.hotmoka.tools;

import io.hotmoka.tools.internal.cli.CreateAccount;
import io.hotmoka.tools.internal.cli.Faucet;
import io.hotmoka.tools.internal.cli.Info;
import io.hotmoka.tools.internal.cli.InitTendermint;
import io.hotmoka.tools.internal.cli.Instrument;
import io.hotmoka.tools.internal.cli.RestartTendermint;
import io.hotmoka.tools.internal.cli.Send;
import io.hotmoka.tools.internal.cli.State;
import io.hotmoka.tools.internal.cli.Verify;
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

	subcommands = { CreateAccount.class,
					CommandLine.HelpCommand.class,
					Info.class,
					InitTendermint.class,
					Instrument.class,
			        Faucet.class,
			        RestartTendermint.class,
			        Send.class,
			        State.class,
			        Verify.class }, 

	description = "This is the Hotmoka CLI",

	showDefaultValues = true

	)
public class CLI {

	public static void main(String[] args) {
		System.exit(new CommandLine(new CLI()).execute(args));
	}
}