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

package io.hotmoka.node.disk.internal;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.local.AbstractStoreTransformation;

/**
 * A transformation of a store of a disk node.
 */
public class DiskStoreTransformation extends AbstractStoreTransformation<DiskNodeImpl, DiskNodeConfig, DiskStore, DiskStoreTransformation> {

	/**
	 * Creates a transformation from an initial store.
	 * 
	 * @param store the initial store
	 * @param consensus the consensus configuration of the node having the store
	 * @param now the time to use as now for the transactions executed in the transformation
	 */
	public DiskStoreTransformation(DiskStore store, ConsensusConfig<?,?> consensus, long now) {
		super(store, consensus, now);
	}
}