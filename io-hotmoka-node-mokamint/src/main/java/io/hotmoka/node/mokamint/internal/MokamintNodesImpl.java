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

import java.nio.file.Files;
import java.security.KeyPair;
import java.util.concurrent.TimeoutException;

import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.mokamint.api.MokamintNode;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.mokamint.application.api.ClosedApplicationException;
import io.mokamint.node.local.AbstractLocalNode;
import io.mokamint.node.local.api.LocalNode;
import io.mokamint.node.local.api.LocalNodeConfig;

/**
 * Providers of blockchain nodes that rely on the Mokamint proof of space engine.
 */
public abstract class MokamintNodesImpl {

	private MokamintNodesImpl() {}

	/**
	 * Creates and starts a node, with a brand new store, of a blockchain based on Mokamint.
	 * It spawns a local Mokamint engine with an application for handling its transactions.
	 * It creates a genesis block and starts mining on top of it (initial synchronization is consequently skipped).
	 * 
	 * @param config the configuration of the Hotmoka node
	 * @param mokamintConfig the configuration of the underlying Mokamint engine
	 * @param keyPair the keys of the Mokamint engine, used to sign the blocks that it mines
	 * @return the Mokamint node
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public static MokamintNode<LocalNode> init(MokamintNodeConfig config, LocalNodeConfig mokamintConfig, KeyPair keyPair) throws InterruptedException {
		var app = new HotmokaApplicationImpl<LocalNode>(config, true);
		AbstractLocalNode engine;

		try {
			engine = new AbstractLocalNode(mokamintConfig, keyPair, app, true) {};
		}
		catch (ClosedApplicationException e) {
			// this is impossible: nobody can have closed our local application
			throw new LocalNodeException(e);
		}

		MokamintNode<LocalNode> node = app.getNode();

		try {
			node.setMokamintEngine(engine);
		}
		catch (io.mokamint.node.api.ClosedNodeException e) {
			// this is impossible: nobody can have closed our local engine
			throw new LocalNodeException(e);
		}
		catch (TimeoutException e) {
			// this is impossible for a local node (such as a MokamintNode) and a local engine
			throw new LocalNodeException(e);
		}

		node.addOnCloseHandler(engine::close);

		return node;
	}

	/**
	 * Creates and starts a Mokamint node that uses an already existing store. The consensus
	 * parameters are recovered from the manifest in the store, hence the store must
	 * be that of an already initialized blockchain. It spawns a local Mokamint engine
	 * and connects it to an application for handling its transactions. It erases
	 * an already existing directory holding a previously created blockchain.
	 * 
	 * @param config the configuration of the Hotmoka node
	 * @param mokamintConfig the configuration of the underlying Mokamint engine
	 * @param keyPair the keys of the Mokamint engine, used to sign the blocks that it mines
	 * @return the Mokamint node
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public static MokamintNode<LocalNode> start(MokamintNodeConfig config, LocalNodeConfig mokamintConfig, KeyPair keyPair) throws InterruptedException {
		var app = new HotmokaApplicationImpl<LocalNode>(config, true);
		AbstractLocalNode engine;

		try {
			engine = new AbstractLocalNode(mokamintConfig, keyPair, app, false) {};
		}
		catch (ClosedApplicationException e) {
			// this is impossible: nobody can have closed our local application
			throw new LocalNodeException(e);
		}

		MokamintNode<LocalNode> node = app.getNode();

		try {
			node.setMokamintEngine(engine);
		}
		catch (io.mokamint.node.api.ClosedNodeException e) {
			// this is impossible: nobody can have closed our local engine
			throw new LocalNodeException(e);
		}
		catch (TimeoutException e) {
			// this is impossible for a local node (such as a MokamintNode) and a local engine
			throw new LocalNodeException(e);
		}

		node.addOnCloseHandler(engine::close);

		return node;
	}

	/**
	 * Creates and starts a Mokamint node that uses an already existing store. The consensus
	 * parameters are recovered from the manifest in the store, hence the store must
	 * be that of an already initialized blockchain. It spawns a local Mokamint engine
	 * and connects it to an application for handling its transactions. It does not erase
	 * an already existing directory holding a previously created blockchain, if it already existed.
	 * 
	 * @param config the configuration of the Hotmoka node
	 * @param mokamintConfig the configuration of the underlying Mokamint engine
	 * @param keyPair the keys of the Mokamint engine, used to sign the blocks that it mines
	 * @return the Mokamint node
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public static MokamintNode<LocalNode> resume(MokamintNodeConfig config, LocalNodeConfig mokamintConfig, KeyPair keyPair) throws InterruptedException {
		var app = new HotmokaApplicationImpl<LocalNode>(config, !Files.exists(config.getDir().resolve("hotmoka")));
		AbstractLocalNode engine;

		try {
			engine = new AbstractLocalNode(mokamintConfig, keyPair, app, false) {};
		}
		catch (ClosedApplicationException e) {
			// this is impossible: nobody can have closed our local application
			throw new LocalNodeException(e);
		}

		MokamintNode<LocalNode> node = app.getNode();

		try {
			node.setMokamintEngine(engine);
		}
		catch (io.mokamint.node.api.ClosedNodeException e) {
			// this is impossible: nobody can have closed our local engine
			throw new LocalNodeException(e);
		}
		catch (TimeoutException e) {
			// this is impossible for a local node (such as a MokamintNode) and a local engine
			throw new LocalNodeException(e);
		}

		node.addOnCloseHandler(engine::close);

		return node;
	}
}