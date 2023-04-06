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

import java.util.stream.Stream;

/**
 * A dictionary of words for BIP39 encoding.
 */
public interface BIP39Dictionary {

	/**
     * Yields the word at the given index inside this dictionary.
     *
     * @param index the index of the word, starting at 0
     * @return the word
     */
	String getWord(int index);

	/**
     * Yields the position of a word inside this dictionary of words.
     *
     * @param word to word to search for
     * @return the position of {@code word} inside this dictionary, starting at 0;
     *         yields a negative number if {@code word} is not contained in this dictionary
     */
	int indexOf(String word);	

	/**
     * Yields all words in this dictionary, in their order.
     *
     * @return the words
     */
	Stream<String> getAllWords();
}