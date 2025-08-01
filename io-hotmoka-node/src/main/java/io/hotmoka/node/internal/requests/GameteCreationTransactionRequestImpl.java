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

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Base64;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.internal.json.TransactionRequestJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * A request for creating an initial gamete, that is, an account of class
 * {@code io.takamaka.code.lang.Gamete} that holds the initial coins of
 * the Hotmoka node.
 */
@Immutable
public class GameteCreationTransactionRequestImpl extends TransactionRequestImpl<GameteCreationTransactionResponse> implements GameteCreationTransactionRequest {
	final static byte SELECTOR = 2;

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous transaction.
	 */
	private final TransactionReference classpath;

	/**
	 * The amount of coins provided to the gamete.
	 */
	private final BigInteger initialAmount;

	/**
	 * The Base64-encoded public key that will be assigned to the gamete.
	 */
	private final String publicKey;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param initialAmount the amount of green coins provided to the gamete
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public GameteCreationTransactionRequestImpl(TransactionReference classpath, BigInteger initialAmount, String publicKey) {
		this(classpath, initialAmount, publicKey, IllegalArgumentException::new);
	}

	/**
	 * Builds a transaction request from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public GameteCreationTransactionRequestImpl(TransactionRequestJson json) throws InconsistentJsonException {
		this(
			Objects.requireNonNull(json.getClasspath(), "classpath cannot be null", InconsistentJsonException::new).unmap(),
			json.getInitialAmount(),
			json.getPublicKey(),
			InconsistentJsonException::new
		);
	}

	/**
	 * Unmarshals a transaction from the given context. The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the unmarshalling failed
	 */
	public GameteCreationTransactionRequestImpl(UnmarshallingContext context) throws IOException {
		this(
			TransactionReferences.from(context),
			context.readBigInteger(),
			context.readStringUnshared(),
			IOException::new
		);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param initialAmount the amount of green coins provided to the gamete
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @throws E if some argument passed to this constructor is illegal
	 */
	private <E extends Exception> GameteCreationTransactionRequestImpl(TransactionReference classpath, BigInteger initialAmount, String publicKey, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		this.classpath = Objects.requireNonNull(classpath, "classpath cannot be null", onIllegalArgs);
		this.initialAmount = Objects.requireNonNull(initialAmount, "initialAmount cannot be null", onIllegalArgs);
		this.publicKey = Base64.requireBase64(Objects.requireNonNull(publicKey, "publicKey cannot be null", onIllegalArgs), onIllegalArgs);
	
		if (initialAmount.signum() < 0)
			throw onIllegalArgs.apply("initialAmount cannot be negative");
	}

	@Override
	public final TransactionReference getClasspath() {
		return classpath;
	}

	@Override
	public final BigInteger getInitialAmount() {
		return initialAmount;
	}

	@Override
	public final String getPublicKey() {
		return publicKey;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  class path: " + classpath + "\n"
        	+ "  initialAmount: " + initialAmount + "\n"
        	+ "  publicKey: " + publicKey;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GameteCreationTransactionRequest gctr &&
			classpath.equals(gctr.getClasspath()) && initialAmount.equals(gctr.getInitialAmount()) &&
			publicKey.equals(gctr.getPublicKey());
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ initialAmount.hashCode() ^ publicKey.hashCode();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		classpath.into(context);
		context.writeBigInteger(initialAmount);
		context.writeStringUnshared(publicKey);
	}
}