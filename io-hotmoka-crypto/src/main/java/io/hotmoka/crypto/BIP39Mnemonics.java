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

import io.hotmoka.crypto.api.BIP39Dictionary;
import io.hotmoka.crypto.api.BIP39Mnemonic;
import io.hotmoka.crypto.internal.BIP39MnemonicImpl;

/**
 * A provider of BIP39 mnemonics.
 */
public final class BIP39Mnemonics {

	private BIP39Mnemonics() {}

	/**
     * Yields the BIP39 mnemonic containing the given words from the given dictionary.
     * If the words were derived from an account, that account can be reconstructed
     * by calling the {@link BIP39Mnemonic#toAccount(java.util.function.BiFunction)} method.
     * 
     * @param words the words, coming from {@code dictionary}
     * @param dictionary the dictionary
     * @return the mnemonic
     */
	public static BIP39Mnemonic of(String[] words, BIP39Dictionary dictionary) {
    	return new BIP39MnemonicImpl(words, dictionary);
    }

    /**
     * Yields the BIP39 mnemonic containing the given words from the English BIP39 dictionary.
     * If the words were derived from an account, that account can be reconstructed
     * by calling the {@link BIP39Mnemonic#toAccount(java.util.function.BiFunction)} method.
     * 
     * @param words the words, coming from English BIP39 dictionary
     * @return the mnemonic
     */
	public static BIP39Mnemonic of(String[] words) {
    	return new BIP39MnemonicImpl(words, BIP39Dictionaries.ENGLISH_DICTIONARY);
    }
}