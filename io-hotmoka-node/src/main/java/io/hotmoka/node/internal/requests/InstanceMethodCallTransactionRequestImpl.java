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

import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a request for calling an instance method of a storage object in a node.
 */
@Immutable
public class InstanceMethodCallTransactionRequestImpl extends AbstractInstanceMethodCallTransactionRequestImpl implements InstanceMethodCallTransactionRequest {
	final static byte SELECTOR = 5;

	// selectors used for calls to coin transfer methods, for their more compact representation
	final static byte SELECTOR_TRANSFER_INT = 7;
	final static byte SELECTOR_TRANSFER_LONG = 8;
	final static byte SELECTOR_TRANSFER_BIG_INTEGER = 9;

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
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public InstanceMethodCallTransactionRequestImpl(Signer<? super InstanceMethodCallTransactionRequestImpl> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals);

		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null");
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
	public InstanceMethodCallTransactionRequestImpl(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals);

		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null");
		this.signature = Objects.requireNonNull(signature, "signature cannot be null");
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
	public InstanceMethodCallTransactionRequestImpl(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		this(NO_SIG, caller, ZERO, "", gasLimit, ZERO, classpath, method, receiver, actuals);
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);
		context.writeLengthAndBytes(signature);
	}

	@Override
	public String toString() {
        return super.toString() + "\n  chainId: " + chainId + "\n  signature: " + Hex.toHexString(signature);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InstanceMethodCallTransactionRequest imctr &&
			super.equals(other) && chainId.equals(imctr.getChainId()) && Arrays.equals(signature, imctr.getSignature());
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
		boolean receiveInt = MethodSignatures.RECEIVE_INT.equals(staticTarget);
		boolean receiveLong = MethodSignatures.RECEIVE_LONG.equals(staticTarget);
		boolean receiveBigInteger = MethodSignatures.RECEIVE_BIG_INTEGER.equals(staticTarget);

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
			getCaller().intoWithoutSelector(context);
			context.writeBigInteger(getGasLimit());
			context.writeBigInteger(getGasPrice());
			getClasspath().into(context);
			context.writeBigInteger(getNonce());
			getReceiver().intoWithoutSelector(context);

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
	public static InstanceMethodCallTransactionRequestImpl from(UnmarshallingContext context, byte selector) throws IOException {
		if (selector == SELECTOR) {
			var chainId = context.readStringUnshared();
			var caller = StorageValues.referenceWithoutSelectorFrom(context);
			var gasLimit = context.readBigInteger();
			var gasPrice = context.readBigInteger();
			var classpath = TransactionReferences.from(context);
			var nonce = context.readBigInteger();
			var actuals = context.readLengthAndArray(StorageValues::from, StorageValue[]::new);
			var method = MethodSignatures.from(context);
			var receiver = StorageValues.referenceWithoutSelectorFrom(context);
			byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

			return new InstanceMethodCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, receiver, actuals);
		}
		else if (selector == SELECTOR_TRANSFER_INT || selector == SELECTOR_TRANSFER_LONG || selector == SELECTOR_TRANSFER_BIG_INTEGER) {
			var chainId = context.readStringUnshared();
			var caller = StorageValues.referenceWithoutSelectorFrom(context);
			var gasLimit = context.readBigInteger();
			var gasPrice = context.readBigInteger();
			var classpath = TransactionReferences.from(context);
			var nonce = context.readBigInteger();
			var receiver = StorageValues.referenceWithoutSelectorFrom(context);

			if (selector == SELECTOR_TRANSFER_INT) {
				int howMuch = context.readInt();
				byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

				return new InstanceMethodCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, MethodSignatures.RECEIVE_INT, receiver, StorageValues.intOf(howMuch));
			}
			else if (selector == SELECTOR_TRANSFER_LONG) {
				long howMuch = context.readLong();
				byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

				return new InstanceMethodCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, MethodSignatures.RECEIVE_LONG, receiver, StorageValues.longOf(howMuch));
			}
			else {
				BigInteger howMuch = context.readBigInteger();
				byte[] signature = context.readLengthAndBytes("Signature length mismatch in request");

				return new InstanceMethodCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, MethodSignatures.RECEIVE_BIG_INTEGER, receiver, StorageValues.bigIntegerOf(howMuch));
			}
		}
		else
			throw new RuntimeException("Unexpected request selector " + selector);
	}
}