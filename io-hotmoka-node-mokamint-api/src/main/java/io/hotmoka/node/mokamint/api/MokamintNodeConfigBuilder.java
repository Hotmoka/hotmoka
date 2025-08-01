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

import io.hotmoka.node.local.api.LocalNodeConfigBuilder;

/**
 * The builder of a configuration of a Mokamint node.
 */
public interface MokamintNodeConfigBuilder extends LocalNodeConfigBuilder<MokamintNodeConfig, MokamintNodeConfigBuilder> {

	/**
	 * Sets the depth of the indexing, that is, the number of uppermost blocks
	 * for which indexing supporting data is maintained. The larger this number,
	 * the more resilient is indexing to large history changes, but higher is
	 * its computational cost and database usage. A negative value means that supporting data
	 * is kept forever, it is never deleted, which protects completely from history changes.
	 */
	MokamintNodeConfigBuilder setIndexingDepth(long indexingDepth);

	/**
	 * Sets the pausing time (in milliseconds) from an indexing iteration to the
	 * next indexing iteration. Reducing this number will make indexing more
	 * reactive to changes in the store, at an increased computational cost.
	 * 
	 * @param indexingPause the pausing time, in milliseconds
	 * @return this builder
	 */
	MokamintNodeConfigBuilder setIndexingPause(long indexingPause);
}