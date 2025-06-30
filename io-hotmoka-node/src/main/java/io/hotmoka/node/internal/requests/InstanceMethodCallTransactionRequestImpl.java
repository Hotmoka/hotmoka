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

import static io.hotmoka.node.MethodSignatures.RECEIVE_BIG_INTEGER;
import static io.hotmoka.node.MethodSignatures.RECEIVE_INT;
import static io.hotmoka.node.MethodSignatures.RECEIVE_LONG;
import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.json.TransactionRequestJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a request for calling an instance method of a storage object in a node.
 */
@Immutable
public class InstanceMethodCallTransactionRequestImpl extends AbstractInstanceMethodCallTransactionRequestImpl implements InstanceMethodCallTransactionRequest {
	final static byte SELECTOR = 5;

	// selectors used for calls to coin transfer methods, for a more compact representation
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
	public InstanceMethodCallTransactionRequestImpl(Signer<? super InstanceMethodCallTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals, IllegalArgumentException::new);

		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null", IllegalArgumentException::new);
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
		this(
			chainId,
			caller,
			gasLimit,
			gasPrice,
			classpath,
			nonce,
			Objects.requireNonNull(actuals, "actuals cannot be null", IllegalArgumentException::new).clone(),
			method,
			receiver,
			Objects.requireNonNull(signature, "signature cannot be null", IllegalArgumentException::new).clone(),
			IllegalArgumentException::new
		);
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
		this(
			"",
			caller,
			gasLimit,
			ZERO,
			classpath,
			ZERO,
			Objects.requireNonNull(actuals, "actuals cannot be null", IllegalArgumentException::new).clone(),
			method,
			receiver,
			NO_SIG,
			IllegalArgumentException::new
		);
	}

	/**
	 * Builds a transaction request from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public InstanceMethodCallTransactionRequestImpl(TransactionRequestJson json) throws InconsistentJsonException {
		this(
			json.getChainId(),
			Objects.requireNonNull(json.getCaller(), "caller cannot be null", InconsistentJsonException::new).unmap().asReference(value -> new InconsistentJsonException("caller must be a storage reference, not a " + value.getClass().getSimpleName())),
			json.getGasLimit(),
			json.getGasPrice(),
			Objects.requireNonNull(json.getClasspath(), "classpath cannot be null", InconsistentJsonException::new).unmap(),
			json.getNonce(),
			convertedActuals(json),
			Objects.requireNonNull(json.getMethod(), "method cannot be null", InconsistentJsonException::new).unmap(),
			Objects.requireNonNull(json.getReceiver(), "receiver cannot be null", InconsistentJsonException::new).unmap().asReference(value -> new InconsistentJsonException("receiver must be a storage reference, not a " + value.getClass().getSimpleName())),
			Hex.fromHexString(Objects.requireNonNull(json.getSignature(), "signature cannot be null", InconsistentJsonException::new), InconsistentJsonException::new),
			InconsistentJsonException::new
		);
	}

	/**
	 * Unmarshals a transaction from the given context. The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @throws IOException if the unmarshalling failed
	 */
	public InstanceMethodCallTransactionRequestImpl(UnmarshallingContext context, byte selector) throws IOException {
		this(
			context.readStringUnshared(),
			StorageReferenceImpl.fromWithoutSelector(context),
			context.readBigInteger(),
			context.readBigInteger(),
			TransactionReferences.from(context),
			context.readBigInteger(),
			unmarshalActuals(context, selector),
			unmarshalMethod(context, selector),
			StorageReferenceImpl.fromWithoutSelector(context),
			context.readLengthAndBytes("Signature length mismatch in request"),
			IOException::new
		);
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
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> InstanceMethodCallTransactionRequestImpl(String chainId, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, BigInteger nonce, StorageValue[] actuals, MethodSignature method, StorageReference receiver, byte[] signature, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals, onIllegalArgs);
	
		this.chainId = Objects.requireNonNull(chainId, "chainId cannot be null", onIllegalArgs);
		this.signature = Objects.requireNonNull(signature, "signature cannot be null", onIllegalArgs);
	}

	private static MethodSignature unmarshalMethod(UnmarshallingContext context, byte selector) throws IOException {
		switch (selector) {
		case SELECTOR: return MethodSignatures.from(context);
		case SELECTOR_TRANSFER_INT: return RECEIVE_INT;
		case SELECTOR_TRANSFER_LONG: return RECEIVE_LONG;
		case SELECTOR_TRANSFER_BIG_INTEGER: return RECEIVE_BIG_INTEGER;
		default: throw new IllegalArgumentException("Unexpected selector " + selector + " for instance method call transaction");
		}
	}

	private static StorageValue[] unmarshalActuals(UnmarshallingContext context, byte selector) throws IOException {
		switch (selector) {
		case SELECTOR: return context.readLengthAndArray(StorageValues::from, StorageValue[]::new);
		case SELECTOR_TRANSFER_INT: return new StorageValue[] { StorageValues.intOf(context.readInt()) };
		case SELECTOR_TRANSFER_LONG: return new StorageValue[] { StorageValues.longOf(context.readLong()) };
		case SELECTOR_TRANSFER_BIG_INTEGER: return new StorageValue[] { StorageValues.bigIntegerOf(context.readBigInteger()) };
		default: throw new IllegalArgumentException("Unexpected selector " + selector + " for instance method call transaction");
		}
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);
		context.writeLengthAndBytes(signature);
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		MethodSignature staticTarget = getStaticTarget();
		boolean receiveInt = RECEIVE_INT.equals(staticTarget);
		boolean receiveLong = RECEIVE_LONG.equals(staticTarget);
		boolean receiveBigInteger = RECEIVE_BIG_INTEGER.equals(staticTarget);
	
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
	
			StorageValue howMuch = actuals().findFirst().get();
	
			if (receiveInt)
				context.writeInt(howMuch.asInt(v -> new IllegalArgumentException("Incorrect argument for " + RECEIVE_INT + ": expected int but found " + v.getClass().getSimpleName())));
			else if (receiveLong)
				context.writeLong(howMuch.asLong(v -> new IllegalArgumentException("Incorrect argument for " + RECEIVE_LONG + ": expected long but found " + v.getClass().getSimpleName())));
			else
				context.writeBigInteger(howMuch.asBigInteger(v -> new IllegalArgumentException("Incorrect argument for " + RECEIVE_BIG_INTEGER + ": expected BigInteger but found " + v.getClass().getSimpleName())));

			getReceiver().intoWithoutSelector(context);
		}
		else
			super.intoWithoutSignature(context);
	}

	@Override
	public String toString() {
        return super.toString() + "\n  chainId: " + chainId + "\n  signature: " + Hex.toHexString(signature);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof InstanceMethodCallTransactionRequestImpl imctri) // optimization
			return super.equals(other) && chainId.equals(imctri.getChainId()) && Arrays.equals(signature, imctri.signature);
		else
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
}