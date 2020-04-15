package io.hotmoka.beans.requests;

import java.io.Serializable;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * A request of a transaction.
 * 
 * @param <R> the type of the response expected for this request
 */
@Immutable
public interface TransactionRequest<R extends TransactionResponse> extends Serializable {
	boolean equals(Object obj);

	int hashCode();

	String toString();
}