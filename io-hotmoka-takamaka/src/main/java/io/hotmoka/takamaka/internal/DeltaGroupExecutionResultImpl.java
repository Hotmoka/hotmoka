package io.hotmoka.takamaka.internal;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.takamaka.DeltaGroupExecutionResult;

/**
 * The result of the execution of a delta group.
 */
class DeltaGroupExecutionResultImpl implements DeltaGroupExecutionResult {
	private final byte[] hash;
	private final List<TransactionResponse> responses;
	private final String id;

	DeltaGroupExecutionResultImpl(byte[] hash, Stream<TransactionResponse> responses, String id) {
		this.hash = hash.clone();
		this.responses = responses.collect(Collectors.toList());
		this.id = id;
	}

	@Override
	public byte[] getHash() {
		return hash.clone();
	}

	@Override
	public Stream<TransactionResponse> responses() {
		return responses.stream();
	}

	@Override
	public String getId() {
		return id;
	}
}