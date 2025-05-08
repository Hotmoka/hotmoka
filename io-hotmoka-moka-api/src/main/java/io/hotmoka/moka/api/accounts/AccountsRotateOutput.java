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

package io.hotmoka.moka.api.accounts;

import java.nio.file.Path;
import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.moka.api.GasCostOutput;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The output of the {@code moka accounts rotate} command.
 */
@Immutable
public interface AccountsRotateOutput extends GasCostOutput {

	/**
	 * Yields the reference to the account whose keys have been rotated.
	 * 
	 * @return the reference to the account whose keys have been rotated; this is missing if the transaction
	 *         has just been posted rather than added, or if the transaction failed
	 */
	Optional<StorageReference> getAccount();

	/**
	 * Yields the path of the new key pair file for the account whose keys have been rotated.
	 * 
	 * @return the path of the new key pair file for the account whose keys have been rotated; this is
	 *         missing if the transaction has just been posted
	 *         rather than added, or if the transaction failed, or if the new key has
	 *         been specified as an explicit public key rather than as a key pair
	 */
	Optional<Path> getFile();
}