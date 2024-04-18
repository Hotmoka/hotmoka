/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.helpers;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.nodes.ConsensusConfig;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.helpers.internal.InitializedNodeImpl;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.ValidatorsConsensusConfig;

/**
 * Providers of nodes where the jar with the basic Takamaka classes have been installed,
 * along with a gamete and a manifest.
 */
@ThreadSafe
public class InitializedNodes {

	private InitializedNodes() {}

	/**
	 * Yields an initialized node with basic Takamaka classes, gamete and manifest.
	 * It uses a generic empty set of validators and a generic gas station.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @return an initialized view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public static InitializedNode of(Node parent, ConsensusConfig<?,?> consensus, Path takamakaCode) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NodeException, TimeoutException, InterruptedException {
		return new InitializedNodeImpl(parent, consensus, takamakaCode, null);
	}

	/**
	 * Yields an initialized node with basic Takamaka classes, gamete and manifest.
	 * Uses the given key pair for controlling the gamete. It allows one to specify how
	 * the validators and the gas station of the node are being created.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param producerOfValidatorsBuilder an algorithm that creates the builder of the validators to be installed in the manifest of the node;
	 *                                    if this is {@code null}, a generic empty validators set is created
	 * @param producerOfGasStation an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *                             if this is {@code null}, a generic gas station is created
	 * @return an initialized view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public static InitializedNode of(Node parent, ValidatorsConsensusConfig<?,?> consensus,
			Path takamakaCode, ProducerOfStorageObject<ValidatorsConsensusConfig<?,?>> producerOfValidatorsBuilder, ProducerOfStorageObject<ConsensusConfig<?,?>> producerOfGasStation) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NodeException, TimeoutException, InterruptedException {
		return new InitializedNodeImpl(parent, consensus, takamakaCode, producerOfValidatorsBuilder, producerOfGasStation);
	}

	/**
	 * An algorithm that yields an object in the store of a node, given
	 * the node and the reference to the basic classes in its store.
	 * 
	 * @param <C> the type of the consensus parameters of the node
	 */
	public interface ProducerOfStorageObject<C extends ConsensusConfig<?,?>> {

		/**
		 * Runs some transactions in the node, that yield the object.
		 * 
		 * @param node the node in whose store the object is being created
		 * @param consensus the consensus parameters of the node
		 * @param takamakaCodeReference the reference to the transaction that installed the Takamaka base classes in the node
		 * @return the reference of the object
		 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
		 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
		 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
		 * @throws SignatureException if some initialization request could not be signed
		 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
		 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
		 * @throws NodeException if the node is not able to perform the operation
		 * @throws TimeoutException if no answer arrives before a time window
		 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
		 */
		StorageReference apply(InitializedNode node, C consensus, TransactionReference takamakaCodeReference)
			throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchAlgorithmException,
					NodeException, TimeoutException, InterruptedException;
	}
}