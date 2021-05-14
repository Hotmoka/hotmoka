/*
Copyright 2021 Fausto Spoto

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