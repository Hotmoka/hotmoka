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

import java.util.Objects;

import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.messages.api.GetClassTagResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

/**
 * Implementation of the network message corresponding to the result of the {@link Node#getClassTag(StorageReference)} method.
 */
public class GetClassTagResultMessageImpl extends AbstractRpcMessage implements GetClassTagResultMessage {

	/**
	 * The result of the call.
	 */
	private final ClassTag result;

	/**
	 * Creates the message.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 */
	public GetClassTagResultMessageImpl(ClassTag result, String id) {
		super(id);

		this.result = Objects.requireNonNull(result, "result cannot be null");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GetClassTagResultMessage gtcrm && super.equals(other) && result.equals(gtcrm.get());
	}

	@Override
	protected String getExpectedType() {
		return GetClassTagResultMessage.class.getName();
	}

	@Override
	public ClassTag get() {
		return result;
	}
}