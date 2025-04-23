/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.moka.internal;

import java.math.BigInteger;
import java.net.URI;

import io.hotmoka.cli.AbstractRpcCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.cli.RpcCommandBody;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.Option;

/**
 * Shared code among the commands that connect to a remote and perform Rpc calls
 * to the public API of the remote.
 */
public abstract class AbstractMokaRpcCommand extends AbstractRpcCommand<RemoteNode, NodeException> {
	protected final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	protected AbstractMokaRpcCommand() {
		super(NodeException.class);
	}

	@Option(names = "--uri", description = "the network URI where the API of the Hotmoka node service is published", defaultValue = "ws://localhost:8001")
	private URI uri;

	/**
	 * Yields the URI of the public API of the remote service.
	 * 
	 * @return the URI
	 */
	protected final URI uri() {
		return uri;
	}

	/**
	 * Opens a remote node connected to the uri of a remote Hotmoka node service, specified through the {@code --uri} option,
	 * and runs the given command body.
	 * 
	 * @param what the body
	 * @throws CommandException if something erroneous must be logged and the user must be informed
	 */
	protected void execute(RpcCommandBody<RemoteNode, NodeException> what) throws CommandException {
		execute(RemoteNodes::of, what, uri);
	}
}