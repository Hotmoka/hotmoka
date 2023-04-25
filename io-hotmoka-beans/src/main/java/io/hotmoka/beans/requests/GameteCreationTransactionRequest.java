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

package io.hotmoka.beans.requests;

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A request for creating an initial gamete. It is an account of class
 * {@code io.takamaka.code.lang.Gamete} that holds the initial coins of the network.
 */
@Immutable
public class GameteCreationTransactionRequest extends InitialTransactionRequest<GameteCreationTransactionResponse> {
	final static byte SELECTOR = 2;

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous transaction.
	 */
	public final TransactionReference classpath;

	/**
	 * The amount of coin provided to the gamete.
	 */

	public final BigInteger initialAmount;

	/**
	 * The amount of red coin provided to the gamete.
	 */

	public final BigInteger redInitialAmount;

	/**
	 * The Base64-encoded public key that will be assigned to the gamete.
	 */
	public final String publicKey;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param initialAmount the amount of green coins provided to the gamete
	 * @param redInitialAmount the amount of red coins provided to the gamete
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public GameteCreationTransactionRequest(TransactionReference classpath, BigInteger initialAmount, BigInteger redInitialAmount, String publicKey) {
		if (classpath == null)
			throw new IllegalArgumentException("classpath cannot be null");

		if (initialAmount == null)
			throw new IllegalArgumentException("initialAmount cannot be null");

		if (initialAmount.signum() < 0)
			throw new IllegalArgumentException("initialAmount cannot be negative");

		if (redInitialAmount == null)
			throw new IllegalArgumentException("redInitialAmount cannot be null");

		if (redInitialAmount.signum() < 0)
			throw new IllegalArgumentException("redInitialAmount cannot be negative");

		if (publicKey == null)
			throw new IllegalArgumentException("publicKey cannot be null");

		this.classpath = classpath;
		this.initialAmount = initialAmount;
		this.redInitialAmount = redInitialAmount;
		this.publicKey = publicKey;
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
		if (other instanceof GameteCreationTransactionRequest) {
			GameteCreationTransactionRequest otherCast = (GameteCreationTransactionRequest) other;
			return classpath.equals(otherCast.classpath) && initialAmount.equals(otherCast.initialAmount) && redInitialAmount.equals(otherCast.redInitialAmount)
				&& publicKey.equals(otherCast.publicKey);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ initialAmount.hashCode() ^ redInitialAmount.hashCode() ^ publicKey.hashCode();
	}

	@Override
	public void into(MarshallingContext context) {
		context.writeByte(SELECTOR);
		classpath.into(context);
		context.writeBigInteger(initialAmount);
		context.writeBigInteger(redInitialAmount);
		context.writeUTF(publicKey);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 */
	public static GameteCreationTransactionRequest from(UnmarshallingContext context) {
		TransactionReference classpath = TransactionReference.from(context);
		BigInteger initialAmount = context.readBigInteger();
		BigInteger redInitialAmount = context.readBigInteger();
		String publicKey = context.readUTF();

		return new GameteCreationTransactionRequest(classpath, initialAmount, redInitialAmount, publicKey);
	}
}