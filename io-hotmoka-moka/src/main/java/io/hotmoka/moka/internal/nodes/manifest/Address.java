/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.internal.nodes.manifest;

import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.remote.api.RemoteNode;
import jakarta.websocket.EncodeException;
import picocli.CommandLine.Command;

@Command(name = "address", description = "Show the address of the manifest of a node.")
public class Address extends AbstractMokaRpcCommand {

	private void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		try {
			var manifest = remote.getManifest();
			System.out.println(json() ? new StorageValues.Encoder().encode(manifest) : manifest);
		}
		catch (EncodeException e) {
			throw new CommandException("Cannot encode in JSON format the address of the manifest of the node at \"" + uri() + "\".", e);
		}
	}

	@Override
	protected void execute() throws CommandException {
		execute(this::body);
	}
}