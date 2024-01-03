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

package io.hotmoka.beans.api;

/**
 * Node-specific information about a Hotmoka node.
 */
public interface NodeInfo {

	/**
	 * Yields the type of the node.
	 */
	String getType();

	/**
	 * Yields the version of the node.
	 */
	String getVersion();

	/**
	 * Yields the identifier of the node inside its network, if any.
	 */
	String getID();

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();
}