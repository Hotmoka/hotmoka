package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.values.StorageReference;

/**
 * A request for a transaction that installs a jar in an initialized node.
 */
@Immutable
public class JarStoreTransactionRequest extends NonInitialTransactionRequest<JarStoreTransactionResponse> implements AbstractJarStoreTransactionRequest {
	final static byte SELECTOR = 3;

	/**
	 * The bytes of the jar to install.
	 */
	private final byte[] jar;

	/**
	 * The dependencies of the jar, already installed in blockchain
	 */
	private final TransactionReference[] dependencies;

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
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains; this can be {@code null}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public JarStoreTransactionRequest(Signer signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws InvalidKeyException, SignatureException {
		super(caller, nonce, chainId, gasLimit, gasPrice, classpath);

		if (jar == null)
			throw new IllegalArgumentException("jar cannot be null");

		if (dependencies == null)
			throw new IllegalArgumentException("dependencies cannot be null");

		for (TransactionReference dependency: dependencies)
			if (dependency == null)
				throw new IllegalArgumentException("dependencies cannot hold null");

		this.jar = jar.clone();
		this.dependencies = dependencies;
		this.signature = signer.sign(this);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 */
	public JarStoreTransactionRequest(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) {
		super(caller, nonce, chainId, gasLimit, gasPrice, classpath);

		this.jar = jar.clone();
		this.dependencies = dependencies;
		this.signature = signature;
	}

	@Override
	public byte[] getSignature() {
		return signature.clone();
	}

	@Override
	public byte[] getJar() {
		return jar.clone();
	}

	@Override
	public int getJarLength() {
		return jar.length;
	}

	@Override
	public Stream<TransactionReference> getDependencies() {
		return Stream.of(dependencies);
	}

	/**
	 * Yields the number of dependencies.
	 * 
	 * @return the number of dependencies
	 */
	public int getNumberOfDependencies() {
		return dependencies.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: jar)
            sb.append(String.format("%02x", b));

        return super.toString() + "\n"
			+ "  dependencies: " + Arrays.toString(dependencies) + "\n"
			+ "  jar: " + sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof JarStoreTransactionRequest) {
			JarStoreTransactionRequest otherCast = (JarStoreTransactionRequest) other;
			return super.equals(otherCast) && Arrays.equals(jar, otherCast.jar) && Arrays.equals(dependencies, otherCast.dependencies);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(jar) ^ Arrays.deepHashCode(dependencies);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(getDependencies().map(gasCostModel::storageCostOf).reduce(BigInteger.ZERO, BigInteger::add))
			.add(gasCostModel.storageCostOfBytes(getJarLength()));
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		super.intoWithoutSignature(context);
		context.oos.writeInt(jar.length);
		context.oos.write(jar);
		intoArray(dependencies, context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static JarStoreTransactionRequest from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		StorageReference caller = StorageReference.from(ois);
		BigInteger gasLimit = unmarshallBigInteger(ois);
		BigInteger gasPrice = unmarshallBigInteger(ois);
		TransactionReference classpath = TransactionReference.from(ois);
		BigInteger nonce = unmarshallBigInteger(ois);
		String chainId = ois.readUTF();

		int jarLength = ois.readInt();
		byte[] jar = new byte[jarLength];
		if (jarLength != ois.readNBytes(jar, 0, jarLength))
			throw new IOException("jar length mismatch in request");

		TransactionReference[] dependencies = unmarshallingOfArray(TransactionReference::from, TransactionReference[]::new, ois);
		byte[] signature = unmarshallSignature(ois);

		return new JarStoreTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, jar, dependencies);
	}
}