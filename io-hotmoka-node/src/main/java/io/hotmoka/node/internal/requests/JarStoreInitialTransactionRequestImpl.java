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
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Base64;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.internal.gson.TransactionRequestJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a request for a transaction that installs a jar in a yet non-initialized node.
 */
@Immutable
public class JarStoreInitialTransactionRequestImpl extends TransactionRequestImpl<JarStoreInitialTransactionResponse> implements JarStoreInitialTransactionRequest {
	final static byte SELECTOR = 1;

	/**
	 * The bytes of the jar to install.
	 */
	private final byte[] jar;

	/**
	 * The dependencies of the jar, already installed in blockchain
	 */
	private final TransactionReference[] dependencies;

	/**
	 * Builds the transaction request.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @throws E if some argument passed to this constructor is illegal
	 */
	public <E extends Exception> JarStoreInitialTransactionRequestImpl(byte[] jar, TransactionReference[] dependencies, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this.jar = Objects.requireNonNull(jar, "jar cannot be null", onIllegalArgs).clone();
		this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null", onIllegalArgs).clone();
		for (var dependency: dependencies)
			Objects.requireNonNull(dependency, "dependencies cannot hold null elements", onIllegalArgs);
	}

	/**
	 * Builds a transaction request from its JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public JarStoreInitialTransactionRequestImpl(TransactionRequestJson json) throws InconsistentJsonException {
		this(Base64.fromBase64String(Objects.requireNonNull(json.getJar(), "jar cannot be null", InconsistentJsonException::new), InconsistentJsonException::new),
			convertedDependencies(json), InconsistentJsonException::new);
	}

	private static TransactionReference[] convertedDependencies(TransactionRequestJson json) throws InconsistentJsonException {
		TransactionReferences.Json[] dependencies = json.getDependencies().toArray(TransactionReferences.Json[]::new);
		var result = new TransactionReference[dependencies.length];
		for (int pos = 0; pos < result.length; pos++)
			result[pos] = Objects.requireNonNull(dependencies[pos], "dependencies cannot hold null elements", InconsistentJsonException::new).unmap();

		return result;
	}

	@Override
	public final byte[] getJar() {
		return jar.clone();
	}

	@Override
	public final int getJarLength() {
		return jar.length;
	}

	@Override
	public final Stream<TransactionReference> getDependencies() {
		return Stream.of(dependencies);
	}

	@Override
	public final int getNumberOfDependencies() {
		return dependencies.length;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
        for (byte b: jar)
            sb.append(String.format("%02x", b));

        return getClass().getSimpleName() + ":\n"
			+ "  dependencies: " + Arrays.toString(dependencies) + "\n"
			+ "  jar: " + sb;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof JarStoreInitialTransactionRequest jsitr &&
			Arrays.equals(dependencies, jsitr.getDependencies().toArray(TransactionReference[]::new)) &&
			Arrays.equals(jar, jsitr.getJar());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(jar) ^ Arrays.deepHashCode(dependencies);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeLengthAndBytes(jar);
		context.writeLengthAndArray(dependencies);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 */
	public static JarStoreInitialTransactionRequest from(UnmarshallingContext context) throws IOException {
		byte[] jar = context.readLengthAndBytes("jar length mismatch in request");
		var dependencies = context.readLengthAndArray(TransactionReferences::from, TransactionReference[]::new);

		return new JarStoreInitialTransactionRequestImpl(jar, dependencies, IOException::new);
	}
}