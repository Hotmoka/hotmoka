package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.internal.UnmarshallingUtils;
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
public interface TransactionRequest<R extends TransactionResponse> extends Serializable {
	boolean equals(Object obj);

	int hashCode();

	String toString();

	/**
	 * Marshals this request into the given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if the request cannot be marshalled
	 */
	void into(ObjectOutputStream oos) throws IOException;

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	static TransactionRequest<?> from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		switch (selector) {
		case ConstructorCallTransactionRequest.SELECTOR: {
			StorageReference caller = (StorageReference) StorageValue.from(ois);
			BigInteger gasLimit = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasPrice = UnmarshallingUtils.unmarshallBigInteger(ois);
			Classpath classpath = Classpath.from(ois);
			BigInteger nonce = UnmarshallingUtils.unmarshallBigInteger(ois);
			int actualsCount = ois.readInt();
			StorageValue[] actuals = new StorageValue[actualsCount];
			for (int pos = 0; pos < actualsCount; pos++)
				actuals[pos] = StorageValue.from(ois);
			ConstructorSignature constructor = (ConstructorSignature) CodeSignature.from(ois);

			return new ConstructorCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, constructor, actuals);
		}
		case GameteCreationTransactionRequest.SELECTOR: {
			Classpath classpath = Classpath.from(ois);
			BigInteger initialAmount = UnmarshallingUtils.unmarshallBigInteger(ois);
			return new GameteCreationTransactionRequest(classpath, initialAmount);
		}
		case InstanceMethodCallTransactionRequest.SELECTOR: {
			StorageReference caller = (StorageReference) StorageValue.from(ois);
			BigInteger gasLimit = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasPrice = UnmarshallingUtils.unmarshallBigInteger(ois);
			Classpath classpath = Classpath.from(ois);
			BigInteger nonce = UnmarshallingUtils.unmarshallBigInteger(ois);
			int actualsCount = ois.readInt();
			StorageValue[] actuals = new StorageValue[actualsCount];
			for (int pos = 0; pos < actualsCount; pos++)
				actuals[pos] = StorageValue.from(ois);
			MethodSignature method = (MethodSignature) CodeSignature.from(ois);
			StorageReference receiver = (StorageReference) StorageValue.from(ois);

			return new InstanceMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals);
		}
		case JarStoreInitialTransactionRequest.SELECTOR: {
			int jarLength = ois.readInt();
			byte[] jar = new byte[jarLength];
			if (jarLength != ois.readNBytes(jar, 0, jarLength))
				throw new IOException("jar length mismatch in request");

			int dependenciesLength = ois.readInt();
			Classpath[] dependencies = new Classpath[dependenciesLength];
			for (int pos = 0; pos < dependenciesLength; pos++)
				dependencies[pos] = Classpath.from(ois);

			return new JarStoreInitialTransactionRequest(jar, dependencies);
		}
		case JarStoreTransactionRequest.SELECTOR: {
			StorageReference caller = (StorageReference) StorageValue.from(ois);
			BigInteger gasLimit = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasPrice = UnmarshallingUtils.unmarshallBigInteger(ois);
			Classpath classpath = Classpath.from(ois);
			BigInteger nonce = UnmarshallingUtils.unmarshallBigInteger(ois);

			int jarLength = ois.readInt();
			byte[] jar = new byte[jarLength];
			if (jarLength != ois.readNBytes(jar, 0, jarLength))
				throw new IOException("jar length mismatch in request");

			int dependenciesLength = ois.readInt();
			Classpath[] dependencies = new Classpath[dependenciesLength];
			for (int pos = 0; pos < dependenciesLength; pos++)
				dependencies[pos] = Classpath.from(ois);

			return new JarStoreTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, jar, dependencies);
		}
		case RedGreenGameteCreationTransactionRequest.SELECTOR: {
			Classpath classpath = Classpath.from(ois);
			BigInteger initialAmount = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger redInitialAmount = UnmarshallingUtils.unmarshallBigInteger(ois);

			return new RedGreenGameteCreationTransactionRequest(classpath, initialAmount, redInitialAmount);
		}
		case StaticMethodCallTransactionRequest.SELECTOR: {
			StorageReference caller = (StorageReference) StorageValue.from(ois);
			BigInteger gasLimit = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasPrice = UnmarshallingUtils.unmarshallBigInteger(ois);
			Classpath classpath = Classpath.from(ois);
			BigInteger nonce = UnmarshallingUtils.unmarshallBigInteger(ois);
			int actualsCount = ois.readInt();
			StorageValue[] actuals = new StorageValue[actualsCount];
			for (int pos = 0; pos < actualsCount; pos++)
				actuals[pos] = StorageValue.from(ois);
			MethodSignature method = (MethodSignature) CodeSignature.from(ois);

			return new StaticMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, actuals);
		}
		default: throw new IOException("unexpected request selector: " + selector);
		}
	}
}