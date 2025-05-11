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

import io.hotmoka.moka.api.nodes.tendermint.validators.NodesTendermintValidatorsKeyOutput;
import io.hotmoka.moka.internal.nodes.tendermint.validators.Key;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka nodes tendermint validators key} command.
 */
public abstract class NodesTendermintValidatorsKeyOutputJson implements JsonRepresentation<NodesTendermintValidatorsKeyOutput> {

	private final String file;

	protected NodesTendermintValidatorsKeyOutputJson(NodesTendermintValidatorsKeyOutput output) {
		this.file = output.getFile().toString();
	}

	public String getFile() {
		return file;
	}

	@Override
	public NodesTendermintValidatorsKeyOutput unmap() throws InconsistentJsonException {
		return new Key.Output(this);
	}
}