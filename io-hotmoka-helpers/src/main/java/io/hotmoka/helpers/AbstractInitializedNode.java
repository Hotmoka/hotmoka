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

package io.hotmoka.helpers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.helpers.internal.InitializedNodeImpl;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;

/**
 * Shared implementation of a node where the jar with the basic Takamaka classes have been installed,
 * along with a gamete and a manifest.
 * 
 * @param <N> the type of the original node that gets decorated with its initialization
 * @param <C> the type of the consensus configuration of the node that gets decorated with its initialization
 */
public abstract class AbstractInitializedNode<N extends Node, C extends ConsensusConfig<?,?>> extends InitializedNodeImpl<N, C> {

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka code in the store of the node is unexpected
	 */
	public AbstractInitializedNode(N parent, C consensus, Path takamakaCode)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, TimeoutException, InterruptedException, ClosedNodeException, UnexpectedCodeException {

		super(parent, consensus, takamakaCode);
	}
}