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

package io.hotmoka.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.hotmoka.beans.internal.marshalling.NodeUnmarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of node's API unmarshalling contexts.
 */
public abstract class NodeUnmarshallingContexts {

	private NodeUnmarshallingContexts() {}

	/**
	 * Yields an unmarshalling context for node's API, that understands
	 * the optimized marshalling obtained through a
	 * {@link NodeMarshallingContexts#of(OutputStream)}.
	 * 
	 * @param is the stream from which bytes get unmarshalled
	 * @throws IOException if the context cannot be created
	 * @return the context
	 */
	public static UnmarshallingContext of(InputStream is) throws IOException {
		return new NodeUnmarshallingContext(is);
	}
}