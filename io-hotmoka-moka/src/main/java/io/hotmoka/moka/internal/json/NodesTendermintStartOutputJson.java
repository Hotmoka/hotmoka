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

import io.hotmoka.moka.api.nodes.tendermint.NodesTendermintStartOutput;
import io.hotmoka.moka.internal.nodes.tendermint.Start;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka nodes tendermint start} command.
 */
public abstract class NodesTendermintStartOutputJson extends NodeStartOutputJson implements JsonRepresentation<NodesTendermintStartOutput> {

	protected NodesTendermintStartOutputJson(NodesTendermintStartOutput output) {
		super(output);
	}

	@Override
	public NodesTendermintStartOutput unmap() throws InconsistentJsonException {
		return new Start.Output(this);
	}
}