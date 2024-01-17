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

package io.hotmoka.node.tendermint;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import io.hotmoka.helpers.InitializedNodes.ProducerOfStorageObject;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.api.SimpleValidatorsConsensusConfig;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.ValidatorsConsensusConfig;
import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.node.tendermint.internal.TendermintInitializedNodeImpl;

/**
 * Providers of Tendermint nodes where the jar with the basic Takamaka classes have been installed,
 * along with a gamete and a manifest.
 */
public abstract class TendermintInitializedNodes {

	private TendermintInitializedNodes() {}

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the chain id and the validators of the underlying Tendermint network. It uses a generic gas station.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	public static InitializedNode of(TendermintNode parent, ValidatorsConsensusConfig<?,?> consensus, Path takamakaCode) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new TendermintInitializedNodeImpl(parent, consensus, null, takamakaCode);
	}

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest. Uses the chain id and the validators
	 * of the underlying Tendermint network. It allows one to specify the gas station to use.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param producerOfGasStation an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *                             if this is {@code null}, a generic gas station is created
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	public static InitializedNode of(TendermintNode parent, SimpleValidatorsConsensusConfig consensus,
			ProducerOfStorageObject<ConsensusConfig<?,?>> producerOfGasStation, Path takamakaCode) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new TendermintInitializedNodeImpl(parent, consensus, producerOfGasStation, takamakaCode);
	}
}