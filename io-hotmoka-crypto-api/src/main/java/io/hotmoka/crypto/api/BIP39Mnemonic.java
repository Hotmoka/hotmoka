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

package io.hotmoka.crypto.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A container of BIP39 words. It can be used to represent just the
 * entropy of an account or all the information about an account.
 */
public interface BIP39Mnemonic {

    /**
     * Yields the words in this container, in their order.
     * 
     * @return the words
     */
    Stream<String> stream();

    /**
     * Yields the account reconstructed from these BIP39 mnemonic words.
     * This works only if the words were actually derived from an account.
     * 
     * @param <R> the type of reference that identifies the account
     * @param accountCreator a function that creates an account from its entropy and from
     *                       the byte representation of its reference
     * @return the account
     */
    <R extends Comparable<? super R>> Account<R> toAccount(BiFunction<Entropy, byte[], Account<R>> accountCreator);

	/**
	 * Dumps these words into a file.
	 * 
	 * @param name the name of the file
	 * @throws IOException if the file cannot be written
	 */
	void dump(Path name) throws IOException;
}