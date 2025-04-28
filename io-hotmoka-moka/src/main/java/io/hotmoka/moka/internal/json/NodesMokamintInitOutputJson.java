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

import java.net.URI;

import io.hotmoka.moka.api.nodes.mokamint.NodesMokamintInitOutput;
import io.hotmoka.moka.internal.nodes.mokamint.Init;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka mokamint init} command.
 */
public abstract class NodesMokamintInitOutputJson extends NodesInitOutputJson implements JsonRepresentation<NodesMokamintInitOutput> {
	private final URI uriMokamintPublic;
	private final URI uriMokamintRestricted;

	protected NodesMokamintInitOutputJson(NodesMokamintInitOutput output) {
		super(output);

		this.uriMokamintPublic = output.getURIMokamintPublic();
		this.uriMokamintRestricted = output.getURIMokamintRestricted();
	}

	public URI getURIMokamintPublic() {
		return uriMokamintPublic;
	}

	public URI getURIMokamintRestricted() {
		return uriMokamintRestricted;
	}

	@Override
	public NodesMokamintInitOutput unmap() throws InconsistentJsonException {
		return new Init.Output(this);
	}
}