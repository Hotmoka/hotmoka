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

import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.node.tendermint.internal.TendermintNodeImpl;

/**
 * Providers of blockchain nodes that rely on a Tendermint process.
 */
public abstract class TendermintNodes {

	private TendermintNodes() {}

	/**
	 * Starts a Tendermint node that uses an already existing store. The consensus
	 * parameters are recovered from the manifest in the store, hence the store must
	 * be that of an already initialized blockchain. It spawns the Tendermint process
	 * and connects it to an ABCI application for handling its transactions.  It erases
	 * the directory holding a previously created blockchain, if any.
	 * 
	 * @param config the configuration of the blockchain
	 * @return the Tendermint node
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public static TendermintNode init(TendermintNodeConfig config) throws InterruptedException {
		return new TendermintNodeImpl(config, true);
	}

	/**
	 * Starts a Tendermint node that uses an already existing store. The consensus
	 * parameters are recovered from the manifest in the store, hence the store must
	 * be that of an already initialized blockchain. It spawns the Tendermint process
	 * and connects it to an ABCI application for handling its transactions.  It does not
	 * erase the directory holding a previously created blockchain, if it already exists.
	 * 
	 * @param config the configuration of the blockchain
	 * @return the Tendermint node
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public static TendermintNode resume(TendermintNodeConfig config) throws InterruptedException {
		return new TendermintNodeImpl(config);
	}
}