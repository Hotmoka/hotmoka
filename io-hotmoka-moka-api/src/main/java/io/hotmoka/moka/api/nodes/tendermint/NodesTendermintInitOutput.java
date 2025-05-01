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

package io.hotmoka.moka.api.nodes.tendermint;

import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.moka.api.nodes.NodesInitOutput;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The output of the {@code moka nodes tendermint init} command.
 */
@Immutable
public interface NodesTendermintInitOutput extends NodesInitOutput {

	/**
	 * Yields the description of the validators of the initialized node.
	 * 
	 * @return the description of the validators
	 */
	Stream<ValidatorDescription> getValidators();

	/**
	 * The description of a validator.
	 */
	@Immutable
	interface ValidatorDescription extends Comparable<ValidatorDescription> {

		/**
		 * Yields the reference of the validator.
		 * 
		 * @return the reference of the validator
		 */
		StorageReference getReference();

		/**
		 * Yields the Base58-encoded public key of the validator.
		 * 
		 * @return the Base58-encoded public key of the validator
		 */
		String getPublicKeyBase58();
	}
}