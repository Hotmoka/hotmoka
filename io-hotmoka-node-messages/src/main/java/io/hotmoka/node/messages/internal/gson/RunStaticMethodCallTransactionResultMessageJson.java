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

import java.util.Optional;

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.internal.RunStaticMethodCallTransactionResultMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of a {@link RunStaticMethodCallTransactionResultMessage}.
 */
public abstract class RunStaticMethodCallTransactionResultMessageJson extends AbstractRpcMessageJsonRepresentation<RunStaticMethodCallTransactionResultMessage> {
	private final StorageValues.Json result;

	protected RunStaticMethodCallTransactionResultMessageJson(RunStaticMethodCallTransactionResultMessage message) {
		super(message);

		this.result = message.get().map(StorageValues.Json::new).orElse(null);
	}

	public final Optional<StorageValues.Json> getResult() {
		return Optional.ofNullable(result);
	}

	@Override
	public RunStaticMethodCallTransactionResultMessage unmap() throws InconsistentJsonException {
		return new RunStaticMethodCallTransactionResultMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return RunStaticMethodCallTransactionResultMessage.class.getName();
	}
}