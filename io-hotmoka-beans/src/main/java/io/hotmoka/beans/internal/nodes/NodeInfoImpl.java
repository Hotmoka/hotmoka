/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.beans.internal.nodes;

import java.util.Objects;

import io.hotmoka.beans.api.NodeInfo;

/**
 * Implementation of node-specific information about a Hotmoka node.
 */
public class NodeInfoImpl implements NodeInfo {

	/**
	 * The type of the node.
	 */
	private final String type;

	/**
	 * The version of the node.
	 */
	private final String version;

	/**
	 * The identifier of the node inside its network, if any.
	 */
	private final String ID;

	/**
	 * Builds node-specific information about a Hotmoka node.
	 * 
	 * @param type the type of the node
	 * @param version the version of the node
	 * @param ID the identifier of the node inside its network, if any. Otherwise the empty string
	 */
	public NodeInfoImpl(String type, String version, String ID) {
		Objects.requireNonNull(type, "type cannot be null");
		Objects.requireNonNull(version, "version cannot be null");
		Objects.requireNonNull(ID, "ID cannot be null");

		this.type = type;
		this.version = version;
		this.ID = ID;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NodeInfo ni && type.equals(ni.getType()) &&
			version.equals(ni.getVersion()) && ID.equals(ni.getID());
	}

	@Override
	public int hashCode() {
		return type.hashCode() ^ version.hashCode() ^ ID.hashCode();
	}

	public String getType() {
		return type;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public String getID() {
		return ID;
	}
}