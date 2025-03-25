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

import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.internal.gson.RunStaticMethodCallTransactionMessageJson;
import io.hotmoka.websockets.beans.AbstractRpcMessage;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of the network message corresponding to {@link Node#runStaticMethodCallTransaction(StaticMethodCallTransactionRequest)}.
 */
public class RunStaticMethodCallTransactionMessageImpl extends AbstractRpcMessage implements RunStaticMethodCallTransactionMessage {

	private final StaticMethodCallTransactionRequest request;

	/**
	 * Creates the message.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	public RunStaticMethodCallTransactionMessageImpl(StaticMethodCallTransactionRequest request, String id) {
		this(request, id, IllegalArgumentException::new);
	}

	/**
	 * Creates the message from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public RunStaticMethodCallTransactionMessageImpl(RunStaticMethodCallTransactionMessageJson json) throws InconsistentJsonException {
		this(unmapRequest(json), json.getId(), InconsistentJsonException::new);
	}

	/**
	 * Creates the message.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> RunStaticMethodCallTransactionMessageImpl(StaticMethodCallTransactionRequest request, String id, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(Objects.requireNonNull(id, "id cannot be null", onIllegalArgs));
	
		this.request = Objects.requireNonNull(request, "request cannot be null", onIllegalArgs);
	}

	private static StaticMethodCallTransactionRequest unmapRequest(RunStaticMethodCallTransactionMessageJson json) throws InconsistentJsonException {
		var unmappedRequest = Objects.requireNonNull(json.getRequest(), "request cannot be null", InconsistentJsonException::new).unmap();

		if (unmappedRequest instanceof StaticMethodCallTransactionRequest smctr)
			return smctr;
		else
			throw new InconsistentJsonException("The argument of the runStaticMethodCallTransactionRequest() method must be a StaticMethodCallTransactionRequest");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof RunStaticMethodCallTransactionMessage rsmctm && super.equals(other) && request.equals(rsmctm.getRequest());
	}

	@Override
	protected String getExpectedType() {
		return RunStaticMethodCallTransactionMessage.class.getName();
	}

	@Override
	public StaticMethodCallTransactionRequest getRequest() {
		return request;
	}
}