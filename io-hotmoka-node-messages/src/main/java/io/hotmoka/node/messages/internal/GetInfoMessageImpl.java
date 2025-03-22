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

package io.hotmoka.node.messages.internal;

import io.hotmoka.node.api.Node;
import io.hotmoka.node.messages.api.GetInfoMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

/**
 * Implementation of the network message corresponding to {@link Node#getInfo()}.
 */
public class GetInfoMessageImpl extends AbstractRpcMessage implements GetInfoMessage {

	/**
	 * Creates the message.
	 * 
	 * @param id the identifier of the message
	 */
	public GetInfoMessageImpl(String id) {
		super(id);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GetInfoMessage && super.equals(other);
	}

	@Override
	protected String getExpectedType() {
		return GetInfoMessage.class.getName();
	}
}