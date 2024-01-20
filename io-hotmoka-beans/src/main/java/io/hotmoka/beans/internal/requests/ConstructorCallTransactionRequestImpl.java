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

package io.hotmoka.beans.internal.requests;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

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
	 * @param signer the signer of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier of the network where the request will be sent
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public ConstructorCallTransactionRequestImpl(Signer<? super ConstructorCallTransactionRequestImpl> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals);

		this.constructor = Objects.requireNonNull(constructor, "constructor cannot be null");
		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null");

		if (constructor.getFormals().count() != actuals.length)
			throw new IllegalArgumentException("Argument count mismatch between formals and actuals");

		this.signature = signer.sign(this);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains; this can be {@code null}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 */
	public ConstructorCallTransactionRequestImpl(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals);

		Objects.requireNonNull(constructor, "constructor cannot be null");
		Objects.requireNonNull(chainId, "chainId cannot be null");
		Objects.requireNonNull(signature, "signature cannot be null");

		if (constructor.getFormals().count() != actuals.length)
			throw new IllegalArgumentException("argument count mismatch between formals and actuals");

		this.constructor = constructor;
		this.chainId = chainId;
		this.signature = signature;
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
		return other instanceof ConstructorCallTransactionRequestImpl cctr && super.equals(other)
			&& constructor.equals(cctr.constructor) && chainId.equals(cctr.chainId) && Arrays.equals(signature, cctr.signature);
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
	public static ConstructorCallTransactionRequestImpl from(UnmarshallingContext context) throws IOException {
		var chainId = context.readStringUnshared();
		var caller = StorageValues.referenceWithoutSelectorFrom(context);
		var gasLimit = context.readBigInteger();
		var gasPrice = context.readBigInteger();
		var classpath = TransactionReferences.from(context);
		var nonce = context.readBigInteger();
		StorageValue[] actuals = context.readLengthAndArray(StorageValues::from, StorageValue[]::new);
		var constructor = ConstructorSignatures.from(context);
		byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

		return new ConstructorCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, constructor, actuals);
	}
}