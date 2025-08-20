/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.node.mokamint.internal;

import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.AbstractNodeDecorator;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.InitializedNodes.ProducerOfStorageObject;
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.mokamint.api.MokamintInitializedNode;
import io.hotmoka.node.mokamint.api.MokamintNode;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * Compared to the {@link io.hotmoka.helpers.api.InitializedNode} interface, this
 * class uses a validators object for Hotmoka nodes based on Mokamint.
 */
public class MokamintInitializedNodeImpl extends AbstractNodeDecorator<InitializedNode> implements MokamintInitializedNode {

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. It uses a generic gas station.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws UnexpectedCodeException if the Takamaka runtime installed in the node contains unexpected code
	 * @throws ClosedNodeException if the node is already closed
	 */
	public MokamintInitializedNodeImpl(MokamintNode<?> parent, ConsensusConfig<?,?> consensus, Path takamakaCode)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, TimeoutException, InterruptedException, ClosedNodeException, UnexpectedCodeException {

		super(mkParent(parent, consensus, null, takamakaCode));
	}

	private static InitializedNode mkParent(MokamintNode<?> parent, ConsensusConfig<?,?> consensus, ProducerOfStorageObject producerOfGasStationBuilder, Path takamakaCode) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, TimeoutException, InterruptedException, ClosedNodeException, UnexpectedCodeException {
		return InitializedNodes.of(parent, consensus, takamakaCode,
			(node, takamakaCodeReference) -> createMokamintValidatorsBuilder(node, consensus, takamakaCodeReference),
			producerOfGasStationBuilder);
	}

	private static StorageReference createMokamintValidatorsBuilder(InitializedNode node, ConsensusConfig<?,?> consensus, TransactionReference takamakaCodeReference)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, UnexpectedCodeException, TimeoutException, InterruptedException {

		StorageReference gamete = node.gamete();
		var getNonceRequest = TransactionRequests.instanceViewMethodCall(gamete, BigInteger.valueOf(50_000), takamakaCodeReference, MethodSignatures.NONCE, gamete);
		BigInteger nonce = node.runInstanceMethodCallTransaction(getNonceRequest)
			.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.NONCE))
			.asReturnedBigInteger(MethodSignatures.NONCE, UnexpectedValueException::new);

		// we create the builder of the validators object
		var _200_000 = BigInteger.valueOf(200_000);
		ClassType builderClass = StorageTypes.classNamed(StorageTypes.MOKAMINT_VALIDATORS + "$Builder");

		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonce, "", _200_000, ZERO, takamakaCodeReference,
				ConstructorSignatures.of(builderClass, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER),
					StorageValues.bigIntegerOf(consensus.getTicketForNewPoll()), StorageValues.bigIntegerOf(consensus.getFinalSupply()),
					StorageValues.bigIntegerOf(consensus.getHeightAtFinalSupply()));

		return node.addConstructorCallTransaction(request);
	}

	@Override
	public StorageReference gamete() throws ClosedNodeException, TimeoutException, InterruptedException {
		ensureNotClosed();
		return getParent().gamete();
	}
}