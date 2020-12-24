package io.hotmoka.beans.requests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

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

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);

		// we add the signature
		byte[] signature = getSignature();
		writeLength(signature.length, context);
		context.oos.write(signature);
	}

	@Override
	public final byte[] toByteArrayWithoutSignature() throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			intoWithoutSignature(new MarshallingContext(oos));
			oos.flush();
			return baos.toByteArray();
		}
	}

	@Override
	public String toString() {
        return super.toString()
       		+ "\n  chainId: " + chainId + "\n"
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
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel)
			.add(gasCostModel.storageCostOfBytes(signature.length))
			.add(gasCostModel.storageCostOf(chainId));
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		MethodSignature staticTarget = getStaticTarget();
		boolean receiveInt = CodeSignature.RECEIVE_INT.equals(staticTarget);
		boolean receiveLong = CodeSignature.RECEIVE_LONG.equals(staticTarget);
		boolean receiveBigInteger = CodeSignature.RECEIVE_BIG_INTEGER.equals(staticTarget);

		if (receiveInt)
			context.oos.writeByte(SELECTOR_TRANSFER_INT);
		else if (receiveLong)
			context.oos.writeByte(SELECTOR_TRANSFER_LONG);
		else if (receiveBigInteger)
			context.oos.writeByte(SELECTOR_TRANSFER_BIG_INTEGER);
		else
			context.oos.writeByte(SELECTOR);

		context.oos.writeUTF(chainId);

		if (receiveInt || receiveLong || receiveBigInteger) {
			caller.intoWithoutSelector(context);
			marshal(gasLimit, context);
			marshal(gasPrice, context);
			classpath.into(context);
			marshal(nonce, context);
			receiver.intoWithoutSelector(context);

			StorageValue howMuch = actuals().findFirst().get();

			if (receiveInt)
				context.oos.writeInt(((IntValue) howMuch).value);
			else if (receiveLong)
				context.oos.writeLong(((LongValue) howMuch).value);
			else
				marshal(((BigIntegerValue) howMuch).value, context);
		}
		else
			super.intoWithoutSignature(context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param ois the stream
	 * @param selector the selector
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static InstanceMethodCallTransactionRequest from(ObjectInputStream ois, byte selector) throws IOException, ClassNotFoundException {
		if (selector == SELECTOR) {
			String chainId = ois.readUTF();
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
			MethodSignature method = (MethodSignature) CodeSignature.from(ois);
			StorageReference receiver = StorageReference.from(ois);
			byte[] signature = unmarshallSignature(ois);

			return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, receiver, actuals);
		}
		else if (selector == SELECTOR_TRANSFER_INT || selector == SELECTOR_TRANSFER_LONG || selector == SELECTOR_TRANSFER_BIG_INTEGER) {
			String chainId = ois.readUTF();
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageReference receiver = StorageReference.from(ois);

			if (selector == SELECTOR_TRANSFER_INT) {
				int howMuch = ois.readInt();
				byte[] signature = unmarshallSignature(ois);

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_INT, receiver, new IntValue(howMuch));
			}
			else if (selector == SELECTOR_TRANSFER_LONG) {
				long howMuch = ois.readLong();
				byte[] signature = unmarshallSignature(ois);

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_LONG, receiver, new LongValue(howMuch));
			}
			else {
				BigInteger howMuch = unmarshallBigInteger(ois);
				byte[] signature = unmarshallSignature(ois);

				return new InstanceMethodCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, CodeSignature.RECEIVE_BIG_INTEGER, receiver, new BigIntegerValue(howMuch));
			}
		}
		else
			throw new InternalFailureException("unexpeced request selector " + selector);
	}
}