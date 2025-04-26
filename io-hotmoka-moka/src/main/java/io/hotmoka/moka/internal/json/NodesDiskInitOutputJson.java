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

package io.hotmoka.moka.internal.json;

import io.hotmoka.moka.api.nodes.disk.NodesDiskInitOutput;
import io.hotmoka.moka.internal.nodes.disk.Init;
import io.hotmoka.node.StorageValues;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka disk init} command.
 */
public abstract class NodesDiskInitOutputJson implements JsonRepresentation<NodesDiskInitOutput> {
	private final StorageValues.Json gamete;

	protected NodesDiskInitOutputJson(NodesDiskInitOutput output) {
		this.gamete = new StorageValues.Json(output.getGamete());
	}

	public StorageValues.Json getManifest() {
		return gamete;
	}

	@Override
	public NodesDiskInitOutput unmap() throws InconsistentJsonException {
		return new Init.Output(this);
	}
}