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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.responses.JarStoreTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.gson.TransactionRequestJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * A request for a transaction that installs a jar in an initialized node.
 */
@Immutable
public class JarStoreTransactionRequestImpl extends NonInitialTransactionRequestImpl<JarStoreTransactionResponse> implements JarStoreTransactionRequest {
	final static byte SELECTOR = 3;

	/**
	 * The bytes of the jar to install.
	 */
	private final byte[] jar;

	/**
	 * The dependencies of the jar, already installed in blockchain
	 */
	private final TransactionReference[] dependencies;

	/**
	 * The chain identifier where this request can be executed, to forbid transaction replay across chains.
	 */
	private final String chainId;

	/**
	 * The signature of the request.
	 */
	private final byte[] signature;

	/**
	 * Builds the transaction request.
	 * 
	 * @param signer the signer of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public JarStoreTransactionRequestImpl(Signer<? super JarStoreTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, IllegalArgumentException::new);

		this.jar = Objects.requireNonNull(jar, "jar cannot be null", IllegalArgumentException::new).clone();

		this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null", IllegalArgumentException::new).clone();
		for (var dependency: dependencies)
			Objects.requireNonNull(dependency, "dependencies cannot hold null", IllegalArgumentException::new);

		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null", IllegalArgumentException::new);
		this.signature = signer.sign(this);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @throws E if some argument passed to this constructor is illegal
	 */
	public <E extends Exception> JarStoreTransactionRequestImpl(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference[] dependencies, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(caller, nonce, gasLimit, gasPrice, classpath, onIllegalArgs);

		this.jar = Objects.requireNonNull(jar, "jar cannot be null", onIllegalArgs).clone();

		this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null", onIllegalArgs).clone();
		for (var dependency: dependencies)
			Objects.requireNonNull(dependency, "dependencies cannot hold null elements", onIllegalArgs);

		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null", onIllegalArgs);
		this.signature = Objects.requireNonNull(signature, "signature cannot be null", onIllegalArgs).clone();
	}

	/**
	 * Builds a transaction request from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public JarStoreTransactionRequestImpl(TransactionRequestJson json) throws InconsistentJsonException {
		this(
			Hex.fromHexString(Objects.requireNonNull(json.getSignature(), "signature cannot be null", InconsistentJsonException::new), InconsistentJsonException::new),
			Objects.requireNonNull(json.getCaller(), "caller cannot be null", InconsistentJsonException::new).unmap().asReference(value -> new InconsistentJsonException("caller must be a storage reference, not a " + value.getClass().getSimpleName())),
			json.getNonce(),
			json.getChainId(),
			json.getGasLimit(),
			json.getGasPrice(),
			Objects.requireNonNull(json.getClasspath(), "classpath cannot be null", InconsistentJsonException::new).unmap(),
			Base64.fromBase64String(Objects.requireNonNull(json.getJar(), "json cannot be null", InconsistentJsonException::new), InconsistentJsonException::new),
			convertedDependencies(json),
			InconsistentJsonException::new
		);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 */
	public static JarStoreTransactionRequest from(UnmarshallingContext context) throws IOException {
		var chainId = context.readStringUnshared();
		var caller = StorageValues.referenceWithoutSelectorFrom(context);
		var gasLimit = context.readBigInteger();
		var gasPrice = context.readBigInteger();
		var classpath = TransactionReferences.from(context);
		var nonce = context.readBigInteger();
	
		byte[] jar = context.readLengthAndBytes("Jar length mismatch in request");
		var dependencies = context.readLengthAndArray(TransactionReferences::from, TransactionReference[]::new);
		byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");
	
		return new JarStoreTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, jar, dependencies, IOException::new);
	}

	private static TransactionReference[] convertedDependencies(TransactionRequestJson json) throws InconsistentJsonException {
		var dependencies = json.getDependencies().toArray(TransactionReferences.Json[]::new);
		var result = new TransactionReference[dependencies.length];
		for (int pos = 0; pos < result.length; pos++)
			result[pos] = Objects.requireNonNull(dependencies[pos], "dependencies cannot hold null elements", InconsistentJsonException::new).unmap();

		return result;
	}

	@Override
	public byte[] getSignature() {
		return signature.clone();
	}

	@Override
	public String getChainId() {
		return chainId;
	}

	@Override
	public byte[] getJar() {
		return jar.clone();
	}

	@Override
	public int getJarLength() {
		return jar.length;
	}

	@Override
	public Stream<TransactionReference> getDependencies() {
		return Stream.of(dependencies);
	}

	@Override
	public int getNumberOfDependencies() {
		return dependencies.length;
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);
		context.writeLengthAndBytes(getSignature());
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
        for (byte b: jar)
            sb.append(String.format("%02x", b));

        return super.toString() + "\n"
        	+ "  chainId: " + chainId + "\n"
			+ "  dependencies: " + Arrays.toString(dependencies) + "\n"
			+ "  jar: " + sb + "\n"
			+ "  signature: " + Hex.toHexString(signature);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof JarStoreTransactionRequestImpl jstri) // optimization
			return super.equals(other)
				&& Arrays.equals(jar, jstri.jar) && Arrays.equals(dependencies, jstri.dependencies)
				&& chainId.equals(jstri.chainId) && Arrays.equals(signature, jstri.signature);
		else
			return other instanceof JarStoreTransactionRequest jstr && super.equals(other)
				&& Arrays.equals(jar, jstr.getJar()) && Arrays.equals(dependencies, jstr.getDependencies().toArray(TransactionReference[]::new))
				&& chainId.equals(jstr.getChainId()) && Arrays.equals(signature, jstr.getSignature());
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(jar) ^ Arrays.deepHashCode(dependencies) ^ chainId.hashCode() ^ Arrays.hashCode(signature);
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeStringUnshared(chainId);
		super.intoWithoutSignature(context);
		context.writeLengthAndBytes(jar);
		context.writeLengthAndArray(dependencies);
	}

	@Override
	public byte[] toByteArrayWithoutSignature() {
		try (var baos = new ByteArrayOutputStream(); var context = NodeMarshallingContexts.of(baos)) {
			intoWithoutSignature(context);
			context.flush();
			return baos.toByteArray();
		}
		catch (IOException e) {
			// impossible with a byte array output stream
			throw new RuntimeException("Unexpected exception", e);
		}
	}
}