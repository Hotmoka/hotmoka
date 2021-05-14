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

package io.takamaka.code.governance;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.View;

/**
 * The validator of a network of nodes. It can be used to
 * collect money when transactions get validated. It is an account
 * with an identity string, that is used to identify validators that
 * must be rewarded or punished at each validation step.
 * The identity must be derived from the public key of the validator,
 * hence it can coincide with that key or can be an abstraction of it.
 */
public class Validator extends ExternallyOwnedAccount {

	/**
	 * Creates a validator with no initial funds.
	 * 
	 * @param publicKey the Base64-encoded public key of the validator
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	public Validator(String publicKey) {
		super(publicKey);
	}

	/**
	 * Yields the identifier of the validator. By default, this is the
	 * public key of the account, but subclasses may redefine.
	 * 
	 * @return the identifier of the validator. This must be derived from the
	 *         public key in a sufficiently distinctive way
	 */
	public @View String id() {
		return publicKey();
	}
}