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

package io.hotmoka.node.internal.requests;

import java.io.IOException;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * Implementation of a request to initialize a node. It sets the manifest of a node.
 * After the manifest has been set, no more initial transactions can be executed,
 * hence the node is considered initialized. The manifest cannot be set twice.
 */
@Immutable
public class InitializationTransactionRequestImpl extends TransactionRequestImpl<InitializationTransactionResponse> implements InitializationTransactionRequest {
	final static byte SELECTOR = 10;

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous transaction.
	 */
	private final TransactionReference classpath;

	/**
	 * The storage reference that must be set as manifest.
	 */
	private final StorageReference manifest;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param manifest the storage reference that must be set as manifest
	 */
	public InitializationTransactionRequestImpl(TransactionReference classpath, StorageReference manifest) {
		this.classpath = Objects.requireNonNull(classpath, "classpath cannot be null");
		this.manifest = Objects.requireNonNull(manifest, "manifest cannot be null");
	}

	@Override
	public TransactionReference getClasspath() {
		return classpath;
	}

	@Override
	public StorageReference getManifest() {
		return manifest;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n" + "  class path: " + classpath + "\n" + "  manifest: " + manifest;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InitializationTransactionRequest itr && classpath.equals(itr.getClasspath()) && manifest.equals(itr.getManifest());
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ manifest.hashCode();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		classpath.into(context);
		manifest.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 */
	public static InitializationTransactionRequest from(UnmarshallingContext context) throws IOException {
		var classpath = TransactionReferences.from(context);
		var manifest = StorageValues.referenceWithoutSelectorFrom(context);

		return TransactionRequests.initialization(classpath, manifest);
	}
}