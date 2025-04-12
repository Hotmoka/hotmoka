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

package io.hotmoka.moka.internal.node.manifest;

import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.Command;

@Command(name = "show", description = "Show the manifest of a node.")
public class Show extends AbstractMokaRpcCommand {

	private void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		if (json())
			throw new CommandException("JSON output is not yet implemented for this command");

		try {
			System.out.println(ManifestHelpers.of(remote));
		}
		catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
			// this should not happen on a working node
			throw new NodeException("A transaction failed while accessing the manifest of the node", e);
		}
	}

	@Override
	protected void execute() throws CommandException {
		execute(this::body);
	}
}