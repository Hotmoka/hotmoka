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

package io.hotmoka.beans.requests;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.BeanUnmarshaller;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;

/**
 * A request for a transaction that installs a jar in a yet not initialized node.
 */
@Immutable
public class JarStoreInitialTransactionRequest extends InitialTransactionRequest<JarStoreInitialTransactionResponse> implements AbstractJarStoreTransactionRequest {
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
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 */
	public JarStoreInitialTransactionRequest(byte[] jar, TransactionReference... dependencies) {
		if (jar == null)
			throw new IllegalArgumentException("jar cannot be null");

		if (dependencies == null)
			throw new IllegalArgumentException("dependencies cannot be null");

		for (TransactionReference dependency: dependencies)
			if (dependency == null)
				throw new IllegalArgumentException("dependencies cannot hold null");

		this.jar = jar.clone();
		this.dependencies = dependencies.clone();
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

	/**
	 * Yields the number of dependencies.
	 * 
	 * @return the number of dependencies
	 */
	public final int getNumberOfDependencies() {
		return dependencies.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: jar)
            sb.append(String.format("%02x", b));

        return getClass().getSimpleName() + ":\n"
			+ "  dependencies: " + Arrays.toString(dependencies) + "\n"
			+ "  jar: " + sb;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof JarStoreInitialTransactionRequest) {
			JarStoreInitialTransactionRequest otherCast = (JarStoreInitialTransactionRequest) other;
			return Arrays.equals(dependencies, otherCast.dependencies) && Arrays.equals(jar, otherCast.jar);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(jar) ^ Arrays.deepHashCode(dependencies);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeInt(jar.length);
		context.write(jar);
		intoArray(dependencies, context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static JarStoreInitialTransactionRequest from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		int jarLength = context.readInt();
		byte[] jar = context.readBytes(jarLength, "jar length mismatch in request");
		TransactionReference[] dependencies = context.readArray((BeanUnmarshaller<TransactionReference>) TransactionReference::from, TransactionReference[]::new);

		return new JarStoreInitialTransactionRequest(jar, dependencies);
	}
}