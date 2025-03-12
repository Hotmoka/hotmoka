/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import io.hotmoka.crypto.api.Signer;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.gson.TransactionRequestDecoder;
import io.hotmoka.node.internal.gson.TransactionRequestEncoder;
import io.hotmoka.node.internal.gson.TransactionRequestJson;
import io.hotmoka.node.internal.requests.ConstructorCallTransactionRequestImpl;
import io.hotmoka.node.internal.requests.GameteCreationTransactionRequestImpl;
import io.hotmoka.node.internal.requests.InitializationTransactionRequestImpl;
import io.hotmoka.node.internal.requests.InstanceMethodCallTransactionRequestImpl;
import io.hotmoka.node.internal.requests.InstanceSystemMethodCallTransactionRequestImpl;
import io.hotmoka.node.internal.requests.JarStoreInitialTransactionRequestImpl;
import io.hotmoka.node.internal.requests.JarStoreTransactionRequestImpl;
import io.hotmoka.node.internal.requests.StaticMethodCallTransactionRequestImpl;
import io.hotmoka.node.internal.requests.TransactionRequestImpl;

/**
 * Providers of transaction requests.
 */
public abstract class TransactionRequests {

	private TransactionRequests() {}

	/**
	 * Yields a transaction request to install a jar in a yet non-initialized node.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @return the request
	 * @throws E if some argument passed to this constructor is illegal
	 */
	public static <E extends Exception> JarStoreInitialTransactionRequest jarStoreInitial(byte[] jar, TransactionReference[] dependencies, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		return new JarStoreInitialTransactionRequestImpl(jar, dependencies, onIllegalArgs);
	}

	/**
	 * Yields a transaction request to create a gamete.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param initialAmount the amount of green coins provided to the gamete
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @return the request
	 * @throws E if some argument passed to this constructor is illegal
	 */
	public static <E extends Exception> GameteCreationTransactionRequest gameteCreation(TransactionReference classpath, BigInteger initialAmount, String publicKey, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		return new GameteCreationTransactionRequestImpl(classpath, initialAmount, publicKey, onIllegalArgs);
	}

	/**
	 * Yields a transaction request to mark the node as initialized.
	 * After this transaction, no more initial transactions can be executed.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param manifest the storage reference that must be set as manifest
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @return the request
	 * @throws E if some argument passed to this constructor is illegal
	 */
	public static <E extends Exception> InitializationTransactionRequest initialization(TransactionReference classpath, StorageReference manifest, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		return new InitializationTransactionRequestImpl(classpath, manifest, onIllegalArgs);
	}

	/**
	 * Yields a transaction request to install a jar in an initialized node.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @return the request
	 * @throws E if some argument passed to this constructor is illegal
	 */
	public static <E extends Exception> JarStoreTransactionRequest jarStore(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference[] dependencies, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		return new JarStoreTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, jar, dependencies, onIllegalArgs);
	}

	/**
	 * Yields a transaction request to install a jar in an initialized node.
	 * 
	 * @param signer the signer of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @return the request
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public static JarStoreTransactionRequest jarStore(Signer<? super JarStoreTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws InvalidKeyException, SignatureException {
		return new JarStoreTransactionRequestImpl(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, jar, dependencies);
	}

	/**
	 * yields a transaction request to call a constructor in a node.
	 * 
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains; this can be {@code null}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the request
	 */
	public static ConstructorCallTransactionRequest constructorCall(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) {
		return new ConstructorCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, constructor, actuals);
	}

	/**
	 * yields a transaction request to call a constructor in a node.
	 * 
	 * @param signer the signer of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier of the network where the request will be sent
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the request
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public static ConstructorCallTransactionRequest constructorCall(Signer<? super ConstructorCallTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		return new ConstructorCallTransactionRequestImpl(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, constructor, actuals);
	}

	/**
	 * Yields a transaction request to call an instance method in a node.
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
	 * @return the request
	 */
	public static InstanceMethodCallTransactionRequest instanceMethodCall(Signer<? super InstanceMethodCallTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		return new InstanceMethodCallTransactionRequestImpl(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, receiver, actuals);
	}

	/**
	 * Yields a transaction request to call an instance method in a node.
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
	 * @return the request
	 */
	public static InstanceMethodCallTransactionRequest instanceMethodCall(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		return new InstanceMethodCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, receiver, actuals);
	}

	/**
	 * Yields a transaction request to call an instance method in a node, that is expected to be annotated as {@code @@View}.
	 * It fixes the signature to a missing signature, the nonce to zero, the chain identifier
	 * to the empty string and the gas price to zero. None of them is used for a view transaction.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 * @return the request
	 */
	public static InstanceMethodCallTransactionRequest instanceViewMethodCall(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		return new InstanceMethodCallTransactionRequestImpl(caller, gasLimit, classpath, method, receiver, actuals);
	}

	/**
	 * Yields a transaction request to call an instance method. It is not signed,
	 * hence it is only used for calls started by the same node.
	 * Users cannot run a transaction from this request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 * @return the request
	 */
	public static InstanceSystemMethodCallTransactionRequest instanceSystemMethodCall(StorageReference caller, BigInteger nonce, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		return new InstanceSystemMethodCallTransactionRequestImpl(caller, nonce, gasLimit, classpath, method, receiver, actuals);
	}

	/**
	 * Yields a transaction request to call a static method in a node.
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
	 * @return the request
	 */
	public static StaticMethodCallTransactionRequest staticMethodCall(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) {
		return new StaticMethodCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, actuals);
	}

	/**
	 * Yields a transaction request to call a static method in a node.
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
	 * @return the request
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public static StaticMethodCallTransactionRequest staticMethodCall(Signer<? super StaticMethodCallTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		return new StaticMethodCallTransactionRequestImpl(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, method, actuals);
	}

	/**
	 * Yields a transaction request to call a static method in a node, that is expected to be annotated as {@code @@View}.
	 * It fixes the signature to a missing signature, the nonce to zero, the chain identifier
	 * to the empty string and the gas price to zero. None of them is used for a view transaction.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 * @return the request
	 */
	public static StaticMethodCallTransactionRequest staticViewMethodCall(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageValue... actuals) {
		return new StaticMethodCallTransactionRequestImpl(caller, gasLimit, classpath, method, actuals);
	}

	/**
	 * Yields a transaction request unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
     */
	public static TransactionRequest<?> from(UnmarshallingContext context) throws IOException {
		return TransactionRequestImpl.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends TransactionRequestEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends TransactionRequestDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends TransactionRequestJson {

    	/**
    	 * Creates the Json representation for the given transaction request.
    	 * 
    	 * @param request the transaction request
    	 */
    	public Json(TransactionRequest<?> request) {
    		super(request);
    	}
    }
}