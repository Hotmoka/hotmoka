package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.takamaka.code.engine.Transaction;

/**
 * A transaction of HotMoka code: it is the execution of a
 * request, that led to a response.
 *
 * @param <Request> the type of the request of this transaction
 * @param <Response> the type of the response of this transaction
 */
@Immutable
public final class AbstractTransaction<Request extends TransactionRequest<Response>, Response extends TransactionResponse> implements Transaction<Request, Response> {

	/**
	 * The request from which this transaction started.
	 */
	public final Request request;

	/**
	 * The response computed from the {@code request}.
	 */
	public final Response response;

	/**
	 * Builds a transaction from the given request.
	 * 
	 * @param request the request
	 * @param response the corresponding response
	 */
	public AbstractTransaction(Request request, Response response) {
		this.request = request;
		this.response = response;
	}

	@Override
	public final Request getRequest() {
		return request;
	}

	@Override
	public final Response getResponse() {
		return response;
	}
}