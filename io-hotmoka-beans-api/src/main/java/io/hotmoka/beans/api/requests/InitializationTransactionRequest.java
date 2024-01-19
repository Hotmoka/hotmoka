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

package io.hotmoka.beans.api.requests;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.responses.InitializationTransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;

/**
 * A request to initialize a node. It sets the manifest of a node.
 * After the manifest has been set, no more initial transactions can be executed,
 * hence the node is considered initialized. The manifest cannot be set twice.
 */
@Immutable
public interface InitializationTransactionRequest extends InitialTransactionRequest<InitializationTransactionResponse> {

	/**
	 * Yields the reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous transaction.
	 * 
	 * @return the reference
	 */
	TransactionReference getClasspath();

	/**
	 * Yields the storage reference that must be set as manifest.
	 * 
	 * @return the storage reference that must be set as manifest
	 */
	StorageReference getManifest();
}