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

package io.hotmoka.node.mokamint;

import java.security.KeyPair;
import java.util.concurrent.TimeoutException;

import io.hotmoka.node.mokamint.api.MokamintNode;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.hotmoka.node.mokamint.internal.MokamintHotmokaApplication;
import io.mokamint.node.local.AbstractLocalNode;
import io.mokamint.node.local.api.LocalNodeConfig;

/**
 * Providers of blockchain nodes that rely on the Mokamint proof of space engine.
 */
public abstract class MokamintNodes {

	private MokamintNodes() {}

	/**
	 * Creates and starts a node, with a brand new store, of a blockchain based on Mokamint.
	 * It spawns the Mokamint engine with an application for handling its transactions.
	 * 
	 * @param config the configuration of the Hotmoka node
	 * @param mokamintConfig the configuration of the underlying Mokamint engine
	 * @param keyPair the keys of the Mokamint node, used to sign the blocks that it mines
	 * @param createGenesis if true, creates a genesis block and starts mining on top of it
	 *                      (initial synchronization is consequently skipped), otherwise it
	 *                      synchronizes, waits
	 *                      for whispered blocks and then starts mining on top of them
	 * @return the Mokamint node
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 * @throws TimeoutException if the operation does not complete in time
	 */
	// TODO: distinguish init (createGenesis==true) and start (createGenesis==false)
	public static MokamintNode init(MokamintNodeConfig config, LocalNodeConfig mokamintConfig, KeyPair keyPair, boolean createGenesis) throws InterruptedException, TimeoutException {
		var app = new MokamintHotmokaApplication(config, true);
		var engine = new AbstractLocalNode(mokamintConfig, keyPair, app, createGenesis) {};
		MokamintNode node = app.getNode();
		node.setMokamintEngine(engine);
		node.addOnCloseHandler(engine::close);

		return node;
	}

	/**
	 * Creates and starts a Mokamint node that uses an already existing store. The consensus
	 * parameters are recovered from the manifest in the store, hence the store must
	 * be that of an already initialized blockchain. It spawns the Mokamint engine
	 * and connects it to an application for handling its transactions.
	 * 
	 * @param config the configuration of the Hotmoka node
	 * @param mokamintConfig the configuration of the underlying Mokamint engine
	 * @param keyPair the keys of the Mokamint node, used to sign the blocks that it mines
	 * @return the Mokamint node
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 * @throws TimeoutException if the operation does not complete in time
	 */
	public static MokamintNode resume(MokamintNodeConfig config, LocalNodeConfig mokamintConfig, KeyPair keyPair) throws InterruptedException, TimeoutException {
		var app = new MokamintHotmokaApplication(config, false);
		var engine = new AbstractLocalNode(mokamintConfig, keyPair, app, false) {};
		MokamintNode node = app.getNode();
		node.setMokamintEngine(engine);
		node.addOnCloseHandler(engine::close);

		return node;
	}
}