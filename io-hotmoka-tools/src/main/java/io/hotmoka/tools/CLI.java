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

package io.hotmoka.tools;

import io.hotmoka.tools.internal.cli.Call;
import io.hotmoka.tools.internal.cli.Create;
import io.hotmoka.tools.internal.cli.CreateAccount;
import io.hotmoka.tools.internal.cli.Faucet;
import io.hotmoka.tools.internal.cli.Info;
import io.hotmoka.tools.internal.cli.InitTendermint;
import io.hotmoka.tools.internal.cli.Install;
import io.hotmoka.tools.internal.cli.Instrument;
import io.hotmoka.tools.internal.cli.PrintExceptionMessageHandler;
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

	subcommands = { Call.class,
					CreateAccount.class,
					Create.class,
					CommandLine.HelpCommand.class,
					Info.class,
					InitTendermint.class,
					Install.class,
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
		System.exit(new CommandLine(new CLI()).setExecutionExceptionHandler(new PrintExceptionMessageHandler()).execute(args));
	}
}