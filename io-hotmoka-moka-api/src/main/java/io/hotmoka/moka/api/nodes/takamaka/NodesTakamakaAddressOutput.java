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

package io.hotmoka.moka.api.nodes.takamaka;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * The output of the {@code moka nodes takamaka address} command.
 */
@Immutable
public interface NodesTakamakaAddressOutput {

	/**
	 * Yields the reference to the jar that contains the basic Takamaka classes.
	 * 
	 * @return the reference to the jar that contains the basic Takamaka classes
	 */
	TransactionReference getTakamakaCode();
}