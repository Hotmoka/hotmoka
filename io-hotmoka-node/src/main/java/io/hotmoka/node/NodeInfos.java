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

package io.hotmoka.node;

import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.internal.gson.NodeInfoDecoder;
import io.hotmoka.node.internal.gson.NodeInfoEncoder;
import io.hotmoka.node.internal.gson.NodeInfoJson;
import io.hotmoka.node.internal.nodes.NodeInfoImpl;

/**
 * Providers of node-specific information about a Hotmoka node.
 */
public abstract class NodeInfos {

	private NodeInfos() {}

	/**
	 * Yields node-specific information about a Hotmoka node.
	 * 
	 * @param type the type of the node
	 * @param version the version of the node
	 * @param ID the identifier of the node inside its network, if any. Otherwise the empty string
	 * @return the node specific information
	 */
	public static NodeInfo of(String type, String version, String ID) {
		return new NodeInfoImpl(type, version, ID);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends NodeInfoEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends NodeInfoDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * JSON representation.
     */
    public static class Json extends NodeInfoJson {

    	/**
    	 * Creates the JSON representation for the given node info.
    	 * 
    	 * @param info the node information
    	 */
    	public Json(NodeInfo info) {
    		super(info);
    	}
    }
}