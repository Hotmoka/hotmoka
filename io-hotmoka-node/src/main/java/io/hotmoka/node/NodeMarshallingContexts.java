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

import java.io.OutputStream;

import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.internal.marshalling.NodeMarshallingContext;

/**
 * Providers of node's API marshalling contexts.
 */
public abstract class NodeMarshallingContexts {

	private NodeMarshallingContexts() {}

	/**
	 * Yields a marshalling context for node's API, more optimized than
	 * a normal context, since it shares subcomponents of the node's API.
	 * 
	 * @param oos the stream where bytes are marshalled.
	 * @return the context
	 */
	public static MarshallingContext of(OutputStream oos) {
		return new NodeMarshallingContext(oos);
	}
}