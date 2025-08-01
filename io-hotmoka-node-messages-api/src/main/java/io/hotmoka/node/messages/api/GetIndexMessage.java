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

package io.hotmoka.node.messages.api;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.websockets.beans.api.RpcMessage;

/**
 * The network message corresponding to {@link Node#getIndex(StorageReference)}.
 */
@Immutable
public interface GetIndexMessage extends RpcMessage {

	/**
	 * Yields the reference to the object whose index is required.
	 * 
	 * @return the reference
	 */
	StorageReference getReference();
}