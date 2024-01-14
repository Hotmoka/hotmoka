/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.network.nodes;

import io.hotmoka.beans.NodeInfos;
import io.hotmoka.beans.api.nodes.NodeInfo;

public class NodeInfoModel {
	public String type;
	public String version;
	public String ID;

	/**
	 * Builds node-specific information about a Hotmoka node.
	 * 
	 * @param input the node info
	 */
	public NodeInfoModel(NodeInfo input) {
		this.type = input.getType();
		this.version = input.getVersion();
		this.ID = input.getID();
	}

    public NodeInfoModel() {}

    public NodeInfo toBean() {
    	return NodeInfos.of(type, version, ID);
    }
}