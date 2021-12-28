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

import io.hotmoka.beans.nodes.NodeInfo;

public class NodeInfoModel {
	public String type;
	public String version;
	public String ID;

	/**
	 * Builds node-specific information about a Hotmoka node.
	 * 
	 * @param type the type of the node
	 * @param version the version of the node
	 * @param ID the identifier of the node inside its network, if any. Otherwise the empty string
	 */
	public NodeInfoModel(NodeInfo input) {
		this.type = input.type;
		this.version = input.version;
		this.ID = input.ID;
	}

    public NodeInfoModel() {}

    public NodeInfo toBean() {
    	return new NodeInfo(type, version, ID);
    }
}