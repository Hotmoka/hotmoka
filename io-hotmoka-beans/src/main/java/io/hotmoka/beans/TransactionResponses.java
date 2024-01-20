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

package io.hotmoka.beans;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.api.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.api.responses.InitializationTransactionResponse;
import io.hotmoka.beans.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.internal.gson.TransactionReferenceDecoder;
import io.hotmoka.beans.internal.gson.TransactionReferenceEncoder;
import io.hotmoka.beans.internal.gson.TransactionReferenceJson;
import io.hotmoka.beans.internal.responses.ConstructorCallTransactionExceptionResponseImpl;
import io.hotmoka.beans.internal.responses.ConstructorCallTransactionFailedResponseImpl;
import io.hotmoka.beans.internal.responses.ConstructorCallTransactionSuccessfulResponseImpl;
import io.hotmoka.beans.internal.responses.GameteCreationTransactionResponseImpl;
import io.hotmoka.beans.internal.responses.InitializationTransactionResponseImpl;
import io.hotmoka.beans.internal.responses.JarStoreInitialTransactionResponseImpl;
import io.hotmoka.beans.internal.responses.JarStoreTransactionFailedResponseImpl;
import io.hotmoka.beans.internal.responses.JarStoreTransactionSuccessfulResponseImpl;
import io.hotmoka.beans.internal.responses.MethodCallTransactionExceptionResponseImpl;
import io.hotmoka.beans.internal.responses.MethodCallTransactionFailedResponseImpl;
import io.hotmoka.beans.internal.responses.MethodCallTransactionSuccessfulResponseImpl;
import io.hotmoka.beans.internal.responses.TransactionResponseImpl;
import io.hotmoka.beans.internal.responses.VoidMethodCallTransactionSuccessfulResponseImpl;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of transaction responses.
 */
public abstract class TransactionResponses {

	private TransactionResponses() {}

	/**
	 * Yields the response of a transaction that installs a jar in a yet non-initialized node.
	 * 
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 * @param dependencies the dependencies of the jar, previously installed in blockchain
	 * @param verificationToolVersion the version of the verification tool
	 * @return the response
	 */
	public static JarStoreInitialTransactionResponse jarStoreInitial(byte[] instrumentedJar, Stream<TransactionReference> dependencies, long verificationToolVersion) {
		return new JarStoreInitialTransactionResponseImpl(instrumentedJar, dependencies, verificationToolVersion);
	}

	/**
	 * Yields the response of a transaction that creates a gamete.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gamete the created gamete
	 * @return the response
	 */
	public static GameteCreationTransactionResponse gameteCreation(Stream<Update> updates, StorageReference gamete) {
		return new GameteCreationTransactionResponseImpl(updates, gamete);
	}

	/**
	 * Yields the response of a transaction that marks the node as initialized.
	 * After this transaction, no more initial transactions can be executed.
	 * 
	 * @return the response
	 */
	public static InitializationTransactionResponse initialization() {
		return new InitializationTransactionResponseImpl();
	}

	/**
	 * Yields the response of a successful transaction that installed a jar in a yet non-initialized node.
	 * 
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 * @param dependencies the dependencies of the jar, previously installed in blockchain
	 * @param verificationToolVersion the version of the verification tool
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @return the response
	 */
	public static JarStoreTransactionSuccessfulResponse jarStoreSuccessful(byte[] instrumentedJar, Stream<TransactionReference> dependencies, long verificationToolVersion, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		return new JarStoreTransactionSuccessfulResponseImpl(instrumentedJar, dependencies, verificationToolVersion, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	/**
	 * Yields the response of a failed transaction that should have installed a jar in a yet non-initialized node.
	 * 
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 * @return the response
	 */
	public static JarStoreTransactionFailedResponse jarStoreFailed(String classNameOfCause, String messageOfCause, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		return new JarStoreTransactionFailedResponseImpl(classNameOfCause, messageOfCause, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
	}

	/**
	 * Yields the response to a transaction that successfully called a constructor, without generating exceptions.
	 * 
	 * @param newObject the object that has been successfully created
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @return the response
	 */
	public static ConstructorCallTransactionSuccessfulResponse constructorCallSuccessful(StorageReference newObject, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		return new ConstructorCallTransactionSuccessfulResponseImpl(newObject, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	/**
	 * Yields the response to a transaction that called a constructor whose execution led to an exception.
	 * 
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param where the program point where the cause exception occurred; this might be {@code null}
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @return the response
	 */
	public static ConstructorCallTransactionExceptionResponse constructorCallException(String classNameOfCause, String messageOfCause, String where, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		return new ConstructorCallTransactionExceptionResponseImpl(classNameOfCause, messageOfCause, where, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	/**
	 * Yields the response to a failed transaction that should have called a constructor.
	 * 
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param where the program point where the cause exception occurred; this might be {@code null}
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 * @return the response
	 */
	public static ConstructorCallTransactionFailedResponse constructorCallFailed(String classNameOfCause, String messageOfCause, String where, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		return new ConstructorCallTransactionFailedResponseImpl(classNameOfCause, messageOfCause, where, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
	}

	/**
	 * Yields the response to a transaction that successfully called a non-{@code void} method, without generating exceptions.
	 * 
	 * @param result the value returned by the method
	 * @param selfCharged true if and only if the called method was annotated as {@code @@SelfCharged}, hence the
	 *                    execution was charged to its receiver
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @return the response
	 */
	public static MethodCallTransactionSuccessfulResponse methodCallSuccessful(StorageValue result, boolean selfCharged, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		return new MethodCallTransactionSuccessfulResponseImpl(result, selfCharged, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	/**
	 * Yields the response to a transaction that successfully called a {@code void} method, without generating exceptions.
	 * 
	 * @param selfCharged true if and only if the called method was annotated as {@code @@SelfCharged}, hence the
	 *                    execution was charged to its receiver
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @return the response
	 */
	public static VoidMethodCallTransactionSuccessfulResponse voidMethodCallSuccessful(boolean selfCharged, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		return new VoidMethodCallTransactionSuccessfulResponseImpl(selfCharged, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	/**
	 * Yields the response to a transaction that called a method whose execution led to an exception.
	 * 
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param where the program point where the cause exception occurred; this might be {@code null}
	 * @param selfCharged true if and only if the called method was annotated as {@code @@SelfCharged}, hence the
	 *                    execution was charged to its receiver
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @return the response
	 */
	public static MethodCallTransactionExceptionResponse methodCallException(String classNameOfCause, String messageOfCause, String where, boolean selfCharged, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		return new MethodCallTransactionExceptionResponseImpl(classNameOfCause, messageOfCause, where, selfCharged, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	/**
	 * Yields the response to a failed transaction that should have called a method.
	 * 
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param where the program point where the cause exception occurred; this might be {@code null}
	 * @param selfCharged true if and only if the called method was annotated as {@code @@SelfCharged}, hence the
	 *                    execution was charged to its receiver
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 * @return the response
	 */
	public static MethodCallTransactionFailedResponse methodCallFailed(String classNameOfCause, String messageOfCause, String where, boolean selfCharged, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		return new MethodCallTransactionFailedResponseImpl(classNameOfCause, messageOfCause, where, selfCharged, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
	}

	/**
	 * Yields a transaction responses unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the transaction response
	 * @throws IOException if the response could not be unmarshalled
     */
	public static TransactionResponse from(UnmarshallingContext context) throws IOException {
		return TransactionResponseImpl.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends TransactionReferenceEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends TransactionReferenceDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends TransactionReferenceJson {

    	/**
    	 * Creates the Json representation for the given transaction reference.
    	 * 
    	 * @param reference the transaction reference
    	 */
    	public Json(TransactionReference reference) {
    		super(reference);
    	}
    }
}