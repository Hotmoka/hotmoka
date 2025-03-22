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

package io.hotmoka.moka.internal.node;

import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.remote.api.RemoteNode;
import jakarta.websocket.EncodeException;
import picocli.CommandLine.Command;

@Command(name = "info", description = "Show node-specific information about a node.")
public class Info extends AbstractMokaRpcCommand {

	private void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		try {
			var info = remote.getInfo();
			System.out.println(json() ? new NodeInfos.Encoder().encode(info) : info);
		}
		catch (EncodeException e) {
			throw new CommandException("Cannot encode in JSON format the local information about the node at \"" + uri() + "\".", e);
		}
	}

	@Override
	protected void execute() throws CommandException {
		execute(this::body);
	}
}