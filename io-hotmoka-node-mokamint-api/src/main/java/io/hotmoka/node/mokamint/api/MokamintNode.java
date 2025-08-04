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

package io.hotmoka.node.mokamint.api;

import java.util.Optional;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.local.api.LocalNode;
import io.mokamint.node.api.PublicNode;

/**
 * A Hotmoka node that relies on the Mokamint proof of space engine.
 */
@ThreadSafe
public interface MokamintNode extends LocalNode<MokamintNodeConfig> {

	/**
	 * Sets the Mokamint engine that must be used by this node.
	 * 
	 * @param engine the Mokamint engine
	 */
	void setMokamintEngine(PublicNode engine);

	/**
	 * Yields the Mokamint engine used by this node, if it has been already set.
	 * 
	 * @return the Mokamint engine, if already set
	 */
	Optional<PublicNode> getMokamintEngine();
}