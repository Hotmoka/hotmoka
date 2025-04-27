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
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.hotmoka.cli.AbstractRpcCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.MappedEncoder;
import io.hotmoka.websockets.beans.api.JsonRepresentation;
import jakarta.websocket.EncodeException;
import picocli.CommandLine.Option;

/**
 * Shared code among the commands that connect to a remote Hotmoka node and perform Rpc calls
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

	@Override
	protected final void execute() throws CommandException {
		execute(RemoteNodes::of, this::body, uri);
	}

	/**
	 * Runs the main body of the command, with a remote connected to the uri of a remote Hotmoka node service,
	 * specified through the {@code --uri} option.
	 * 
	 * @throws TimeoutException if the execution times out
	 * @throws InterruptedException if the execution gets interrupted before completion
	 * @throws NodeException if the node is misbehaving
	 * @throws CommandException if something erroneous must be logged and the user must be informed
	 */
	protected abstract void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException;

	/**
	 * Reports on the standard output the given output of a command.
	 * 
	 * @param <O> the type of the output to report
	 * @param json true if and only if the output must be reported in JSON format
	 * @param output the output to report
	 * @param encoder a supplier of a converter of the output into JSON representation; this will
	 *                be used only if {@code json} is true
	 * @throws CommandException if reporting failed
	 */
	protected <O> void report(boolean json, O output, Supplier<MappedEncoder<O, ? extends JsonRepresentation<O>>> encoder) throws CommandException {
		if (json) {
			try {
				System.out.println(encoder.get().encode(output));
			}
			catch (EncodeException e) {
				throw new CommandException("Cannot encode the output of the command in JSON format", e);
			}
		}
		else
			System.out.print(output);
	}
}