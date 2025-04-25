/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.moka.internal.nodes.takamaka;

import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.nodes.takamaka.NodesTakamakaAddressOutput;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.Command;

@Command(name = "address", description = "Show the transaction that installed the Takamaka runtime in a node.")
public class Address extends AbstractMokaRpcCommand {

	private void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		var takamaka = remote.getTakamakaCode();
		new Output(takamaka).println(System.out, json());
	}

	@Override
	protected void execute() throws CommandException {
		execute(this::body);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements NodesTakamakaAddressOutput {
		private final String reference;

		private Output(TransactionReference reference) {
			this.reference = reference.toString();
		}

		/**
		 * Yields the output of this command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 */
		public static Output of(String json) {
			return new Gson().fromJson(json, Output.class);
		}

		@Override
		public void println(PrintStream out, boolean json) {
			if (json)
				out.println(new Gson().toJson(this));
			else
				out.println(reference);
		}
	}
}