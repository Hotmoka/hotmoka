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

package io.hotmoka.node.disk;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.disk.api.DiskNode;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.disk.internal.DiskNodeImpl;
import io.hotmoka.node.local.NodeCreationException;

/**
 * Providers of blockchain nodes that store, sequentially, transactions in a directory
 * on disk memory. They are only meant for experimentation and testing. They do not
 * form a real blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 */
@ThreadSafe
public abstract class DiskNodes {

	private DiskNodes() {}

	/**
	 * Creates a brand new node whose store is kept in disk memory.
	 * 
	 * @param config the configuration of the node
	 * @return the node
	 * @throws NodeCreationException if the node could not be created
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public static DiskNode init(DiskNodeConfig config) throws NodeCreationException, InterruptedException {
		return new DiskNodeImpl(config);
	}
}