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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.gson.TransactionRequestJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

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
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param manifest the storage reference that must be set as manifest
	 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	public <E extends Exception> InitializationTransactionRequestImpl(TransactionReference classpath, StorageReference manifest, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this.classpath = Objects.requireNonNull(classpath, "classpath cannot be null", onIllegalArgs);
		this.manifest = Objects.requireNonNull(manifest, "manifest cannot be null", onIllegalArgs);
	}

	/**
	 * Builds a transaction request from its JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public InitializationTransactionRequestImpl(TransactionRequestJson json) throws InconsistentJsonException {
		this(
			Objects.requireNonNull(json.getClasspath(), "classpath cannot be null", InconsistentJsonException::new).unmap(),
			Objects.requireNonNull(json.getManifest(), "manifest cannot be null", InconsistentJsonException::new).unmap().asReference
				(value -> new InconsistentJsonException("manifest should be a storage reference, not a " + value.getClass().getName())),
			InconsistentJsonException::new
		);
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

		return new InitializationTransactionRequestImpl(classpath, manifest, IOException::new);
	}
}