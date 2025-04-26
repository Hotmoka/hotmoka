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

import io.hotmoka.moka.api.nodes.NodesInfoOutput;
import io.hotmoka.moka.internal.nodes.Info;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka nodes config show} command.
 */
public abstract class NodesInfoOutputJson implements JsonRepresentation<NodesInfoOutput> {
	private final NodeInfos.Json info;

	protected NodesInfoOutputJson(NodesInfoOutput output) {
		this.info = new NodeInfos.Json(output.getInfo());
	}

	public final NodeInfos.Json getInfo() {
		return info;
	}

	@Override
	public NodesInfoOutput unmap() throws InconsistentJsonException {
		return new Info.Output(this);
	}
}