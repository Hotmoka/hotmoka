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

import io.hotmoka.constants.Constants;
import io.hotmoka.tools.internal.moka.BindKey;
import io.hotmoka.tools.internal.moka.Burn;
import io.hotmoka.tools.internal.moka.Call;
import io.hotmoka.tools.internal.moka.Create;
import io.hotmoka.tools.internal.moka.CreateAccount;
import io.hotmoka.tools.internal.moka.CreateKey;
import io.hotmoka.tools.internal.moka.Faucet;
import io.hotmoka.tools.internal.moka.ImportAccount;
import io.hotmoka.tools.internal.moka.Info;
import io.hotmoka.tools.internal.moka.InitMemory;
import io.hotmoka.tools.internal.moka.InitTendermint;
import io.hotmoka.tools.internal.moka.Install;
import io.hotmoka.tools.internal.moka.Instrument;
import io.hotmoka.tools.internal.moka.Mint;
import io.hotmoka.tools.internal.moka.PrintExceptionMessageHandler;
import io.hotmoka.tools.internal.moka.ResumeTendermint;
import io.hotmoka.tools.internal.moka.Send;
import io.hotmoka.tools.internal.moka.ShowAccount;
import io.hotmoka.tools.internal.moka.StartTendermint;
import io.hotmoka.tools.internal.moka.State;
import io.hotmoka.tools.internal.moka.Verify;
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

	subcommands = { BindKey.class,
					Burn.class,
					Call.class,
					Create.class,
					CreateAccount.class,
					CreateKey.class,
					CommandLine.HelpCommand.class,
					Faucet.class,
					ImportAccount.class,
					Info.class,
					InitMemory.class,
					InitTendermint.class,
					Install.class,
					Instrument.class,
					Mint.class,
			        ResumeTendermint.class,
			        Send.class,
			        ShowAccount.class,
			        StartTendermint.class,
			        State.class,
			        Verify.class }, 

	description = "This is version " + Constants.VERSION + " of the Hotmoka command-line interface.",

	showDefaultValues = true

	)
public class Moka {

	public static void main(String[] args) {
		System.exit(new CommandLine(new Moka()).setExecutionExceptionHandler(new PrintExceptionMessageHandler()).execute(args));
	}

	public static void run(String command) {
		new CommandLine(new Moka()).setExecutionExceptionHandler(new PrintExceptionMessageHandler()).execute(command.split(" "));
	}
}