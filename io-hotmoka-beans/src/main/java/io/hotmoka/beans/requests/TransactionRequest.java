package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request of a transaction.
 * 
 * @param <R> the type of the response expected for this request
 */
@Immutable
public abstract class TransactionRequest<R extends TransactionResponse> extends Marshallable {

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static TransactionRequest<?> from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		switch (selector) {
		case ConstructorCallTransactionRequest.SELECTOR: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
			ConstructorSignature constructor = (ConstructorSignature) CodeSignature.from(ois);
			int signatureLength = readLength(ois);
			byte[] signature = new byte[signatureLength];
			if (signatureLength != ois.readNBytes(signature, 0, signatureLength))
				throw new IOException("signature length mismatch in request");

			return new ConstructorCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, constructor, signature, actuals);
		}
		case GameteCreationTransactionRequest.SELECTOR: {
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger initialAmount = unmarshallBigInteger(ois);
			String publicKey = ois.readUTF();
			return new GameteCreationTransactionRequest(classpath, initialAmount, publicKey);
		}
		case InitializationTransactionRequest.SELECTOR: {
			TransactionReference classpath = TransactionReference.from(ois);
			StorageReference manifest = StorageReference.from(ois);
			return new InitializationTransactionRequest(classpath, manifest);
		}
		case InstanceMethodCallTransactionRequest.SELECTOR: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
			MethodSignature method = (MethodSignature) CodeSignature.from(ois);
			StorageReference receiver = StorageReference.from(ois);
			int signatureLength = readLength(ois);
			byte[] signature = new byte[signatureLength];
			if (signatureLength != ois.readNBytes(signature, 0, signatureLength))
				throw new IOException("signature length mismatch in request");

			return new InstanceMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, signature, actuals);
		}
		case JarStoreInitialTransactionRequest.SELECTOR: {
			int jarLength = ois.readInt();
			byte[] jar = new byte[jarLength];
			if (jarLength != ois.readNBytes(jar, 0, jarLength))
				throw new IOException("jar length mismatch in request");

			TransactionReference[] dependencies = unmarshallingOfArray(TransactionReference::from, TransactionReference[]::new, ois);

			return new JarStoreInitialTransactionRequest(jar, dependencies);
		}
		case JarStoreTransactionRequest.SELECTOR: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);

			int jarLength = ois.readInt();
			byte[] jar = new byte[jarLength];
			if (jarLength != ois.readNBytes(jar, 0, jarLength))
				throw new IOException("jar length mismatch in request");

			TransactionReference[] dependencies = unmarshallingOfArray(TransactionReference::from, TransactionReference[]::new, ois);

			int signatureLength = readLength(ois);
			byte[] signature = new byte[signatureLength];
			if (signatureLength != ois.readNBytes(signature, 0, signatureLength))
				throw new IOException("signature length mismatch in request");

			return new JarStoreTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, jar, signature, dependencies);
		}
		case RedGreenGameteCreationTransactionRequest.SELECTOR: {
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger initialAmount = unmarshallBigInteger(ois);
			BigInteger redInitialAmount = unmarshallBigInteger(ois);
			String publicKey = ois.readUTF();

			return new RedGreenGameteCreationTransactionRequest(classpath, initialAmount, redInitialAmount, publicKey);
		}
		case StaticMethodCallTransactionRequest.SELECTOR: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
			MethodSignature method = (MethodSignature) CodeSignature.from(ois);
			int signatureLength = readLength(ois);
			byte[] signature = new byte[signatureLength];
			if (signatureLength != ois.readNBytes(signature, 0, signatureLength))
				throw new IOException("signature length mismatch in request");

			return new StaticMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, signature, actuals);
		}
		case TransferTransactionRequest.SELECTOR_TRANSFER_INT: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageReference receiver = StorageReference.from(ois);
			int howMuch = ois.readInt();
			int signatureLength = readLength(ois);
			byte[] signature = new byte[signatureLength];
			if (signatureLength != ois.readNBytes(signature, 0, signatureLength))
				throw new IOException("signature length mismatch in request");

			return new TransferTransactionRequest(caller, nonce, gasPrice, classpath, receiver, howMuch, signature);
		}
		case TransferTransactionRequest.SELECTOR_TRANSFER_LONG: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageReference receiver = StorageReference.from(ois);
			long howMuch = ois.readLong();
			int signatureLength = readLength(ois);
			byte[] signature = new byte[signatureLength];
			if (signatureLength != ois.readNBytes(signature, 0, signatureLength))
				throw new IOException("signature length mismatch in request");

			return new TransferTransactionRequest(caller, nonce, gasPrice, classpath, receiver, howMuch, signature);
		}
		case TransferTransactionRequest.SELECTOR_TRANSFER_BIG_INTEGER: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			TransactionReference classpath = TransactionReference.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageReference receiver = StorageReference.from(ois);
			BigInteger howMuch = unmarshallBigInteger(ois);
			int signatureLength = readLength(ois);
			byte[] signature = new byte[signatureLength];
			if (signatureLength != ois.readNBytes(signature, 0, signatureLength))
				throw new IOException("signature length mismatch in request");

			return new TransferTransactionRequest(caller, nonce, gasPrice, classpath, receiver, howMuch, signature);
		}
		default: throw new IOException("unexpected request selector: " + selector);
		}
	}

	/**
	 * Checks that this request is syntactically valid.
	 * 
	 * @throws TransactionRejectedException if this request is not syntactically valid
	 */
	public void check() throws TransactionRejectedException {
	}
}