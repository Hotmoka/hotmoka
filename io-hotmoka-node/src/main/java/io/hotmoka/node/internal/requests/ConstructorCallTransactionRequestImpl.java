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
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.stream.Collectors;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.gson.TransactionRequestJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a request for calling a constructor of a storage class in a node.
 */
@Immutable
public class ConstructorCallTransactionRequestImpl extends CodeExecutionTransactionRequestImpl<ConstructorCallTransactionResponse> implements ConstructorCallTransactionRequest {
	final static byte SELECTOR = 4;

	/**
	 * The constructor to call.
	 */
	private final ConstructorSignature constructor;

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
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param signer the signer of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier of the network where the request will be sent
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @throws E if some argument passed to this constructor is illegal
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public <E extends Exception> ConstructorCallTransactionRequestImpl(Signer<? super ConstructorCallTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue[] actuals, ExceptionSupplier<? extends E> onIllegalArgs) throws E, InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals, onIllegalArgs);

		this.constructor = Objects.requireNonNull(constructor, "constructor cannot be null", onIllegalArgs);
		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null", onIllegalArgs);

		if (constructor.getFormals().count() != actuals.length)
			throw onIllegalArgs.apply("Argument count mismatch: " + constructor.getFormals().count() + " formals vs " + actuals.length + " actuals");

		this.signature = signer.sign(this);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains; this can be {@code null}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @throws E if some argument passed to this constructor is illegal
	 */
	public <E extends Exception> ConstructorCallTransactionRequestImpl(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue[] actuals, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals, onIllegalArgs);

		this.constructor = Objects.requireNonNull(constructor, "constructor cannot be null", onIllegalArgs);
		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null", onIllegalArgs);
		this.signature = Objects.requireNonNull(signature, "signature cannot be null", onIllegalArgs);

		if (constructor.getFormals().count() != actuals.length)
			throw onIllegalArgs.apply("Argument count mismatch between formals and actuals");
	}

	/**
	 * Builds a transaction request from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public ConstructorCallTransactionRequestImpl(TransactionRequestJson json) throws InconsistentJsonException {
		this(
			Hex.fromHexString(Objects.requireNonNull(json.getSignature(), "signature cannot be null", InconsistentJsonException::new), InconsistentJsonException::new),
			Objects.requireNonNull(json.getCaller(), "caller cannot be null", InconsistentJsonException::new).unmap().asReference(value -> new InconsistentJsonException("caller must be a storage reference, not a " + value.getClass().getSimpleName())),
			json.getNonce(),
			json.getChainId(),
			json.getGasLimit(),
			json.getGasPrice(),
			Objects.requireNonNull(json.getClasspath(), "classpath cannot be null", InconsistentJsonException::new).unmap(),
			Objects.requireNonNull(json.getConstructor(), "constructor cannot be null", InconsistentJsonException::new).unmap(),
			convertedActuals(json),
			InconsistentJsonException::new
		);
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);
		context.writeLengthAndBytes(getSignature());
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
        	+ "  chainId: " + chainId + "\n"
			+ "  constructor: " + constructor + "\n"
			+ "  actuals:\n" + actuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", "")) + "\n"
			+ "  signature: " + Hex.toHexString(signature);
	}

	@Override
	public ConstructorSignature getStaticTarget() {
		return constructor;
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
	public boolean equals(Object other) {
		return other instanceof ConstructorCallTransactionRequest cctr && super.equals(other)
			&& constructor.equals(cctr.getStaticTarget()) && chainId.equals(cctr.getChainId())
			&& Arrays.equals(signature, cctr.getSignature());
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ constructor.hashCode() ^ chainId.hashCode() ^ Arrays.hashCode(signature);
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeStringUnshared(chainId);
		super.intoWithoutSignature(context);
		constructor.into(context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request cannot be unmarshalled
	 */
	public static ConstructorCallTransactionRequest from(UnmarshallingContext context) throws IOException {
		var chainId = context.readStringUnshared();
		var caller = StorageValues.referenceWithoutSelectorFrom(context);
		var gasLimit = context.readBigInteger();
		var gasPrice = context.readBigInteger();
		var classpath = TransactionReferences.from(context);
		var nonce = context.readBigInteger();
		var actuals = context.readLengthAndArray(StorageValues::from, StorageValue[]::new);
		var constructor = ConstructorSignatures.from(context);
		byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

		return new ConstructorCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, constructor, actuals, IOException::new);
	}
}