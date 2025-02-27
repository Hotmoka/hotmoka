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

import io.hotmoka.node.Updates;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.messages.GetClassTagResultMessages;
import io.hotmoka.node.messages.api.GetClassTagResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of a {@link GetClassTagResultMessage}.
 */
public abstract class GetClassTagResultMessageJson extends AbstractRpcMessageJsonRepresentation<GetClassTagResultMessage> {
	private final Updates.Json result;

	protected GetClassTagResultMessageJson(GetClassTagResultMessage message) {
		super(message);

		this.result = new Updates.Json(message.get());
	}

	@Override
	public GetClassTagResultMessage unmap() throws InconsistentJsonException {
		var unmappedResult = result.unmap();
		if (unmappedResult instanceof ClassTag ct)
			return GetClassTagResultMessages.of(ct, getId());
		else
			throw new InconsistentJsonException("The return value of a getClassTag() call must be a class tag");
	}

	@Override
	protected String getExpectedType() {
		return GetClassTagResultMessage.class.getName();
	}
}