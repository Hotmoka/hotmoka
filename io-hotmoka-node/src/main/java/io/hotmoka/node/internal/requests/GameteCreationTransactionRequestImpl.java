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
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.TransactionReferences;

/**
 * A request for creating an initial gamete, that is, an account of class
 * {@code io.takamaka.code.lang.Gamete} that holds the initial coins of the network.
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
	 * The amount of red coins provided to the gamete.
	 */
	private final BigInteger redInitialAmount;

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
	 * @param redInitialAmount the amount of red coins provided to the gamete
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public GameteCreationTransactionRequestImpl(TransactionReference classpath, BigInteger initialAmount, BigInteger redInitialAmount, String publicKey) {
		this.classpath = Objects.requireNonNull(classpath, "classpath cannot be null");
		this.initialAmount = Objects.requireNonNull(initialAmount, "initialAmount cannot be null");
		this.redInitialAmount = Objects.requireNonNull(redInitialAmount, "redInitialAmount cannot be null");
		this.publicKey = Objects.requireNonNull(publicKey, "publicKey cannot be null");

		if (initialAmount.signum() < 0)
			throw new IllegalArgumentException("initialAmount cannot be negative");

		if (redInitialAmount.signum() < 0)
			throw new IllegalArgumentException("redInitialAmount cannot be negative");
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
	public final BigInteger getRedInitialAmount() {
		return redInitialAmount;
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
        	+ "  redInitialAmount: " + redInitialAmount + "\n"
        	+ "  publicKey: " + publicKey;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GameteCreationTransactionRequest gctr &&
			classpath.equals(gctr.getClasspath()) && initialAmount.equals(gctr.getInitialAmount()) &&
			redInitialAmount.equals(gctr.getRedInitialAmount()) && publicKey.equals(gctr.getPublicKey());
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ initialAmount.hashCode() ^ redInitialAmount.hashCode() ^ publicKey.hashCode();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		classpath.into(context);
		context.writeBigInteger(initialAmount);
		context.writeBigInteger(redInitialAmount);
		context.writeStringUnshared(publicKey);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the unmarshalling failed
	 */
	public static GameteCreationTransactionRequestImpl from(UnmarshallingContext context) throws IOException {
		var classpath = TransactionReferences.from(context);
		var initialAmount = context.readBigInteger();
		var redInitialAmount = context.readBigInteger();
		var publicKey = context.readStringUnshared();

		return new GameteCreationTransactionRequestImpl(classpath, initialAmount, redInitialAmount, publicKey);
	}
}