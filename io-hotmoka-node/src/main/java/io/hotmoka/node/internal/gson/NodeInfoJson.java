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

package io.hotmoka.node.internal.gson;

import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link NodeInfo}.
 */
public abstract class NodeInfoJson implements JsonRepresentation<NodeInfo> {
	private final String type;
	private final String version;
	private final String ID;

	protected NodeInfoJson(NodeInfo info) {
		this.type = info.getType();
		this.version = info.getVersion();
		this.ID = info.getID();
	}

	@Override
	public NodeInfo unmap() {
		return NodeInfos.of(type, version, ID);
	}
}