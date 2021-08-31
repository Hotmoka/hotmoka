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

import java.util.stream.Stream;

import io.hotmoka.crypto.internal.BIP39WordsImpl;

/**
 * A container of BIP39 words. It can be used to represent just the
 * entropy of an account or all the information abour an account.
 */
public interface BIP39Words {

    /**
     * Yields the BIP39 words containing the given words from the given dictionary.
     * If the words were derived from an account, that account can be reconstructed
     * by calling the {@link #toAccount()} method.
     * 
     * @param words the words, coming from {@code dictionary}
     * @param dictionary the dictionary
     */
    static BIP39Words of(String[] words, BIP39Dictionary dictionary) {
    	return new BIP39WordsImpl(words, dictionary);
    }

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
     * @return the account
     */
    Account toAccount();
}