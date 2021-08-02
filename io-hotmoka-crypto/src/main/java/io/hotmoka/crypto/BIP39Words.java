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

public interface BIP39Words {

    static BIP39Words of(Account account, BIP39Dictionary dictionary) {
    	return new BIP39WordsImpl(account, dictionary);
    }

    static BIP39Words of(byte[] entropy, BIP39Dictionary dictionary) {
    	return new BIP39WordsImpl(entropy, dictionary);
    }

    static BIP39Words of(String[] words, BIP39Dictionary dictionary) {
    	return new BIP39WordsImpl(words, dictionary);
    }

    Stream<String> stream();

    /**
     * Yields the account reconstructed from these BIP39 mnemonic words.
     *
     * @return the account
     */
    Account toAccount();
}