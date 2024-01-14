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
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A request for calling an instance method of a storage object in a node.
 */
@Immutable
public class InstanceMethodCallTransactionRequest extends AbstractInstanceMethodCallTransactionRequest implements SignedTransactionRequest {
	final static byte SELECTOR = 5;

	// selectors used for calls to coin transfer methods, for their more compact representation
	final static byte SELECTOR_TRANSFER_INT = 7;
	final static byte SELECTOR_TRANSFER_LONG = 8;
	final static byte SELECTOR_TRANSFER_BIG_INTEGER = 9;

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
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public InstanceMethodCallTransactionRequest(Signer<? super InstanceMethodCallTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals);

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
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	public InstanceMethodCallTransactionRequest(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals);

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
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	public InstanceMethodCallTransactionRequest(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		this(NO_SIG, caller, ZERO, "", gasLimit, ZERO, classpath, method, receiver, actuals);
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);
		context.writeLengthAndBytes(signature); // we add the signature
	}

	@Override
	public String toString() {
        return super.toString() + "\n  chainId: " + chainId + "\n  signature: " + bytesToHex(signature);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InstanceMethodCallTransactionRequest imctr &&
			super.equals(other) && chainId.equals(imctr.chainId) && Arrays.equals(signature, imctr.signature);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ chainId.hashCode() ^ Arrays.hashCode(signature);
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
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		MethodSignature staticTarget = getStaticTarget();
		boolean receiveInt = CodeSignature.RECEIVE_INT.equals(staticTarget);
		boolean receiveLong = CodeSignature.RECEIVE_LONG.equals(staticTarget);
		boolean receiveBigInteger = CodeSignature.RECEIVE_BIG_INTEGER.equals(staticTarget);

		if (receiveInt)
			context.writeByte(SELECTOR_TRANSFER_INT);
		else if (receiveLong)
			context.writeByte(SELECTOR_TRANSFER_LONG);
		else if (receiveBigInteger)
			context.writeByte(SELECTOR_TRANSFER_BIG_INTEGER);
		else
			context.writeByte(SELECTOR);

		context.writeStringUnshared(chainId);

		if (receiveInt || receiveLong || receiveBigInteger) {
			caller.intoWithoutSelector(context);
			context.writeBigInteger(gasLimit);
			context.writeBigInteger(gasPrice);
			classpath.into(context);
			context.writeBigInteger(nonce);
			receiver.intoWithoutSelector(context);

			StorageValue howMuch = actuals().findFirst().get();

			if (receiveInt)
				context.writeInt(((IntValue) howMuch).getValue());
			else if (receiveLong)
				context.writeLong(((LongValue) howMuch).getValue());
			else
				context.writeBigInteger(((BigIntegerValue) howMuch).getValue());
		}
		else
			super.intoWithoutSignature(context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 */
	public static InstanceMethodCallTransactionRequest from(UnmarshallingContext context, byte selector) throws IOException {
		if (selector == SELECTOR) {
			var chainId = context.readStringUnshared();
			var caller = StorageReference.from(context);
			var gasLimit = context.readBigInteger();
			var gasPrice = context.readBigInteger();
			var classpath = TransactionReference.from(context);
			var nonce = context.readBigInteger();
			var actuals = context.readLengthAndArray(StorageValues::from, StorageValue[]::new);
			var method = (MethodSignature) CodeSignature.from(context);
			var receiver = StorageReference.from(context);
			byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

			return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, receiver, actuals);
		}
		else if (selector == SELECTOR_TRANSFER_INT || selector == SELECTOR_TRANSFER_LONG || selector == SELECTOR_TRANSFER_BIG_INTEGER) {
			var chainId = context.readStringUnshared();
			var caller = StorageReference.from(context);
			var gasLimit = context.readBigInteger();
			var gasPrice = context.readBigInteger();
			var classpath = TransactionReference.from(context);
			var nonce = context.readBigInteger();
			var receiver = StorageReference.from(context);

			if (selector == SELECTOR_TRANSFER_INT) {
				int howMuch = context.readInt();
				byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_INT, receiver, StorageValues.intOf(howMuch));
			}
			else if (selector == SELECTOR_TRANSFER_LONG) {
				long howMuch = context.readLong();
				byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_LONG, receiver, StorageValues.longOf(howMuch));
			}
			else {
				BigInteger howMuch = context.readBigInteger();
				byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_BIG_INTEGER, receiver, StorageValues.bigIntegerOf(howMuch));
			}
		}
		else
			throw new RuntimeException("Unexpected request selector " + selector);
	}
}