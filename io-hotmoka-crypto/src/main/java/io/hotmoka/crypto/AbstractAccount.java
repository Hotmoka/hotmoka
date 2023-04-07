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

package io.hotmoka.crypto;

import java.io.IOException;

import io.hotmoka.crypto.internal.AbstractAccountImpl;

/**
 * Partial implementation of the information to control an account.
 * One needs the entropy from which the key pair can be reconstructed and
 * potentially also a reference that might not be derived from the key,
 * such as, in Hotmoka, the address of the account in the store of the node.
 * 
 * @param <R> the type of reference that identifies the account
 */
public abstract class AbstractAccount<R extends Comparable<? super R>> extends AbstractAccountImpl<R> {

	/**
	 * Creates the information to control an account.
	 * 
	 * @param entropy the entropy, from which the key pair can be derived
	 * @param reference the reference to the account
	 */
	protected AbstractAccount(io.hotmoka.crypto.api.Entropy entropy, R reference) {
		super(entropy, reference);
	}

	/**
	 * Creates the information to control an account.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account
	 * @throws IOException if the PEM file cannot be read
	 */
	protected AbstractAccount(R reference) throws IOException {
		super(reference);
	}

	/**
	 * Creates the information to control an account.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account
	 * @param dir the directory where the PEM file must be looked for
	 * @throws IOException if the PEM file cannot be read
	 */
	protected AbstractAccount(R reference, String dir) throws IOException {
		super(reference, dir);
	}
}