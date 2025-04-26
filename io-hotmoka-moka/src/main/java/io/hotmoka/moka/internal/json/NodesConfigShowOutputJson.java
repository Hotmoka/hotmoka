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

import java.security.NoSuchAlgorithmException;

import io.hotmoka.moka.api.nodes.config.NodesConfigShowOutput;
import io.hotmoka.moka.internal.nodes.config.Show;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka nodes config show} command.
 */
public abstract class NodesConfigShowOutputJson implements JsonRepresentation<NodesConfigShowOutput> {
	private final ConsensusConfigBuilders.Json config;

	protected NodesConfigShowOutputJson(NodesConfigShowOutput output) {
		this.config = new ConsensusConfigBuilders.Json(output.getConfig());
	}

	public final ConsensusConfigBuilders.Json getConfig() {
		return config;
	}

	@Override
	public NodesConfigShowOutput unmap() throws InconsistentJsonException, NoSuchAlgorithmException {
		return new Show.Output(this);
	}
}