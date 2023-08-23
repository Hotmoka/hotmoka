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

package io.hotmoka.memory;

import java.io.IOException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.memory.internal.MemoryBlockchainImpl;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;

/**
 * An implementation of a blockchain that stores, sequentially, transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 * It provides support for the creation of a given number of initial accounts.
 */
@ThreadSafe
public interface MemoryBlockchain extends Node {

	/**
	 * Creates a brand new blockchain in disk memory.
	 * 
	 * @param config the configuration of the blockchain
	 * @return the blockchain
	 * @throws IOException 
	 */
	static MemoryBlockchain init(MemoryBlockchainConfig config, ConsensusParams consensus) throws IOException {
		return new MemoryBlockchainImpl(config, consensus);
	}

	/**
	 * Yields the configuration of the node.
	 * 
	 * @return the configuration
	 */
	MemoryBlockchainConfig getConfig();
}