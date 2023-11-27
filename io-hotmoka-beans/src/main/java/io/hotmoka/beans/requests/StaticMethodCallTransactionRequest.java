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

import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A request for calling a static method of a storage class in a node.
 */
@Immutable
public class StaticMethodCallTransactionRequest extends MethodCallTransactionRequest implements SignedTransactionRequest {
	final static byte SELECTOR = 6;

	/**
	 * The chain identifier where this request can be executed, to forbid transaction replay across chains.
	 */
	public final String chainId;

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
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public StaticMethodCallTransactionRequest(Signer<? super StaticMethodCallTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, actuals);

		Objects.requireNonNull(chainId, "chainId cannot be null");

		this.chainId = chainId;
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
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 */
	public StaticMethodCallTransactionRequest(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, actuals);

		Objects.requireNonNull(chainId, "chainId cannot be null");
		Objects.requireNonNull(signature, "signature cannot be null");
		this.chainId = chainId;
		this.signature = signature;
	}

	/**
	 * Builds the transaction request as it can be sent to run a {@code @@View} method.
	 * It fixes the signature to a missing signature, the nonce to zero, the chain identifier
	 * to the empty string and the gas price to zero. None of them is used for a view transaction.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 */
	public StaticMethodCallTransactionRequest(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageValue... actuals) {
		this(NO_SIG, caller, ZERO, "", gasLimit, ZERO, classpath, method, actuals);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StaticMethodCallTransactionRequest smctr && super.equals(other)
			&& chainId.equals(smctr.chainId) && Arrays.equals(signature, smctr.signature);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ chainId.hashCode() ^ Arrays.hashCode(signature);
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);
		// we add the signature
		context.writeLengthAndBytes(signature);
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeStringUnshared(chainId);
		super.intoWithoutSignature(context);
	}

	@Override
	public String toString() {
        return super.toString() + ":\n" + "  chainId: " + chainId + "\n" + "  signature: " + bytesToHex(signature);
	}

	@Override
	public byte[] getSignature() {
		return signature.clone();
	}

	@Override
	public String getChainId() {
		return chainId;
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 */
	public static StaticMethodCallTransactionRequest from(UnmarshallingContext context) throws IOException {
		var chainId = context.readStringUnshared();
		var caller = StorageReference.from(context);
		var gasLimit = context.readBigInteger();
		var gasPrice = context.readBigInteger();
		var classpath = TransactionReference.from(context);
		var nonce = context.readBigInteger();
		var actuals = context.readLengthAndArray(StorageValue::from, StorageValue[]::new);
		var method = (MethodSignature) CodeSignature.from(context);
		byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

		return new StaticMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, actuals);
	}
}