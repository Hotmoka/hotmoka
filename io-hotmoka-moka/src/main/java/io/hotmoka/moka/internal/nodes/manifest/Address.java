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
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.NodesManifestAddressOutputs;
import io.hotmoka.moka.api.nodes.manifest.NodesManifestAddressOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.json.NodesManifestAddressOutputJson;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;

@Command(name = "address", description = "Show the address of the manifest of a node.")
public class Address extends AbstractMokaRpcCommand {

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		report(json(), new Output(remote.getManifest()), NodesManifestAddressOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements NodesManifestAddressOutput {
		private final StorageReference manifest;

		private Output(StorageReference manifest) {
			this.manifest = manifest;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesManifestAddressOutputJson json) throws InconsistentJsonException {
			this.manifest = Objects.requireNonNull(json.getManifest(), "manifest cannot be null", InconsistentJsonException::new).unmap()
				.asReference(value -> new InconsistentJsonException("The reference to the manifest must be a storage reference, not a " + value.getClass().getName()));
		}

		@Override
		public StorageReference getManifest() {
			return manifest;
		}

		@Override
		public String toString() {
			return manifest.toString() + "\n";
		}
	}
}