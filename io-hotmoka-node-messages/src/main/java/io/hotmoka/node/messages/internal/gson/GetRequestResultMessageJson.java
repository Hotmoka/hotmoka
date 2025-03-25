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

package io.hotmoka.node.messages.internal.gson;

import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.messages.api.GetRequestResultMessage;
import io.hotmoka.node.messages.internal.GetRequestResultMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of a {@link GetRequestResultMessage}.
 */
public abstract class GetRequestResultMessageJson extends AbstractRpcMessageJsonRepresentation<GetRequestResultMessage> {
	private final TransactionRequests.Json result;

	protected GetRequestResultMessageJson(GetRequestResultMessage message) {
		super(message);

		this.result = new TransactionRequests.Json(message.get());
	}

	public final TransactionRequests.Json getResult() {
		return result;
	}

	@Override
	public GetRequestResultMessage unmap() throws InconsistentJsonException {
		return new GetRequestResultMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return GetRequestResultMessage.class.getName();
	}
}