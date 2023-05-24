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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
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
	public InstanceMethodCallTransactionRequest(Signer signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals);

		if (chainId == null)
			throw new IllegalArgumentException("chainId cannot be null");

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

		if (chainId == null)
			throw new IllegalArgumentException("chainId cannot be null");

		if (signature == null)
			throw new IllegalArgumentException("signature cannot be null");

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

		// we add the signature
		byte[] signature = getSignature();
		context.writeCompactInt(signature.length);
		context.write(signature);
	}

	@Override
	public String toString() {
        return super.toString()
       		+ "\n  chainId: " + chainId
        	+ "\n  signature: " + bytesToHex(signature);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof InstanceMethodCallTransactionRequest) {
			InstanceMethodCallTransactionRequest otherCast = (InstanceMethodCallTransactionRequest) other;
			return super.equals(other) && chainId.equals(otherCast.chainId) && Arrays.equals(signature, otherCast.signature);
		}
		else
			return false;
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

		context.writeUTF(chainId);

		if (receiveInt || receiveLong || receiveBigInteger) {
			caller.intoWithoutSelector(context);
			context.writeBigInteger(gasLimit);
			context.writeBigInteger(gasPrice);
			classpath.into(context);
			context.writeBigInteger(nonce);
			receiver.intoWithoutSelector(context);

			StorageValue howMuch = actuals().findFirst().get();

			if (receiveInt)
				context.writeInt(((IntValue) howMuch).value);
			else if (receiveLong)
				context.writeLong(((LongValue) howMuch).value);
			else
				context.writeBigInteger(((BigIntegerValue) howMuch).value);
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
			String chainId = context.readUTF();
			StorageReference caller = StorageReference.from(context);
			BigInteger gasLimit = context.readBigInteger();
			BigInteger gasPrice = context.readBigInteger();
			TransactionReference classpath = TransactionReference.from(context);
			BigInteger nonce = context.readBigInteger();
			StorageValue[] actuals = context.readArray(StorageValue::from, StorageValue[]::new);
			MethodSignature method = (MethodSignature) CodeSignature.from(context);
			StorageReference receiver = StorageReference.from(context);
			byte[] signature = unmarshallSignature(context);

			return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, receiver, actuals);
		}
		else if (selector == SELECTOR_TRANSFER_INT || selector == SELECTOR_TRANSFER_LONG || selector == SELECTOR_TRANSFER_BIG_INTEGER) {
			String chainId = context.readUTF();
			StorageReference caller = StorageReference.from(context);
			BigInteger gasLimit = context.readBigInteger();
			BigInteger gasPrice = context.readBigInteger();
			TransactionReference classpath = TransactionReference.from(context);
			BigInteger nonce = context.readBigInteger();
			StorageReference receiver = StorageReference.from(context);

			if (selector == SELECTOR_TRANSFER_INT) {
				int howMuch = context.readInt();
				byte[] signature = unmarshallSignature(context);

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_INT, receiver, new IntValue(howMuch));
			}
			else if (selector == SELECTOR_TRANSFER_LONG) {
				long howMuch = context.readLong();
				byte[] signature = unmarshallSignature(context);

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_LONG, receiver, new LongValue(howMuch));
			}
			else {
				BigInteger howMuch = context.readBigInteger();
				byte[] signature = unmarshallSignature(context);

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_BIG_INTEGER, receiver, new BigIntegerValue(howMuch));
			}
		}
		else
			throw new RuntimeException("unexpected request selector " + selector);
	}
}