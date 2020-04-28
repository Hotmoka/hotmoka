package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
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
			Classpath classpath = Classpath.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
			ConstructorSignature constructor = (ConstructorSignature) CodeSignature.from(ois);

			return new ConstructorCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, constructor, actuals);
		}
		case GameteCreationTransactionRequest.SELECTOR: {
			Classpath classpath = Classpath.from(ois);
			BigInteger initialAmount = unmarshallBigInteger(ois);
			return new GameteCreationTransactionRequest(classpath, initialAmount);
		}
		case InstanceMethodCallTransactionRequest.SELECTOR: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			Classpath classpath = Classpath.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
			MethodSignature method = (MethodSignature) CodeSignature.from(ois);
			StorageReference receiver = StorageReference.from(ois);

			return new InstanceMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals);
		}
		case JarStoreInitialTransactionRequest.SELECTOR: {
			int jarLength = ois.readInt();
			byte[] jar = new byte[jarLength];
			if (jarLength != ois.readNBytes(jar, 0, jarLength))
				throw new IOException("jar length mismatch in request");

			Classpath[] dependencies = unmarshallingOfArray(Classpath::from, Classpath[]::new, ois);

			return new JarStoreInitialTransactionRequest(jar, dependencies);
		}
		case JarStoreTransactionRequest.SELECTOR: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			Classpath classpath = Classpath.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);

			int jarLength = ois.readInt();
			byte[] jar = new byte[jarLength];
			if (jarLength != ois.readNBytes(jar, 0, jarLength))
				throw new IOException("jar length mismatch in request");

			Classpath[] dependencies = unmarshallingOfArray(Classpath::from, Classpath[]::new, ois);

			return new JarStoreTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, jar, dependencies);
		}
		case RedGreenGameteCreationTransactionRequest.SELECTOR: {
			Classpath classpath = Classpath.from(ois);
			BigInteger initialAmount = unmarshallBigInteger(ois);
			BigInteger redInitialAmount = unmarshallBigInteger(ois);

			return new RedGreenGameteCreationTransactionRequest(classpath, initialAmount, redInitialAmount);
		}
		case StaticMethodCallTransactionRequest.SELECTOR: {
			StorageReference caller = StorageReference.from(ois);
			BigInteger gasLimit = unmarshallBigInteger(ois);
			BigInteger gasPrice = unmarshallBigInteger(ois);
			Classpath classpath = Classpath.from(ois);
			BigInteger nonce = unmarshallBigInteger(ois);
			StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
			MethodSignature method = (MethodSignature) CodeSignature.from(ois);

			return new StaticMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, actuals);
		}
		default: throw new IOException("unexpected request selector: " + selector);
		}
	}
}