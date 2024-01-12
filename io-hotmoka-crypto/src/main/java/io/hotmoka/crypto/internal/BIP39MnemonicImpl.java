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

package io.hotmoka.crypto.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import io.hotmoka.crypto.api.Account;
import io.hotmoka.crypto.api.BIP39Dictionary;
import io.hotmoka.crypto.api.BIP39Mnemonic;
import io.hotmoka.crypto.api.Entropy;

/**
 * An implementation of the BIP39 words computation.
 */
public class BIP39MnemonicImpl implements BIP39Mnemonic {
	private final BIP39Dictionary dictionary;
    private final String[] words;

    /**
     * Creates the BIP39 words for the given account using the given dictionary.
     * 
     * @param account the account
     * @param dictionary the dictionary
     */
    public BIP39MnemonicImpl(Account<?> account, BIP39Dictionary dictionary) {
    	this.dictionary = dictionary;

    	byte[] entropy = account.getEntropyAsBytes();
    	byte[] reference = account.getReferenceAsBytes();
    	byte[] merge = new byte[entropy.length + reference.length];
    	System.arraycopy(entropy, 0, merge, 0, entropy.length);
    	System.arraycopy(reference, 0, merge, entropy.length, reference.length);
        this.words = words(merge, new ArrayList<>());
    }

    /**
     * Creates the BIP39 words for the given entropy using the given dictionary.
     * 
     * @param entropy the entropy
     * @param dictionary the dictionary
     */
    BIP39MnemonicImpl(byte[] entropy, BIP39Dictionary dictionary) {
    	this.dictionary = dictionary;
        this.words = words(entropy, new ArrayList<>());
    }

    /**
     * Creates the BIP39 words containing the given words from the given dictionary.
     * 
     * @param words the words, coming from {@code dictionary}
     * @param dictionary the dictionary
     */
    public BIP39MnemonicImpl(String[] words, BIP39Dictionary dictionary) {
    	this.words = words.clone();
    	this.dictionary = dictionary;
    }

    @Override
    public Stream<String> stream() {
        return Stream.of(words);
    }

    @Override
    public <R extends Comparable<? super R>> Account<R> toAccount(BiFunction<Entropy, byte[], Account<R>> accountCreator) {
        // each mnemonic word represents 11 bits
        var bits = new boolean[words.length * 11];
        
        // the transaction is always 32 bytes long
        var transaction = new byte[32];

        int bitsOfChecksum = words.length / 3;
        var checksum = new boolean[bitsOfChecksum];

        // the entropy uses the remaining number of bytes
        var entropy = new byte[(bits.length - bitsOfChecksum) / 8 - transaction.length];

        int startOfTransaction = entropy.length * 8;
        int startOfChecksum = startOfTransaction + transaction.length * 8;

        // we transform the mnemonic phrase into a sequence of bits
        int pos = 0;
        for (String word: words) {
            int index = dictionary.indexOf(word);
            if (index < 0)
                throw new IllegalArgumentException(word + " is not a valid mnemonic word");

            // every word accounts for 11 bits
            for (int bit = 0; bit <= 10; bit++)
                bits[pos++] = (index & (0x400 >>> bit)) != 0;
        }

        // the first startOfTransaction bits are the entropy
        for (pos = 0; pos < startOfTransaction; pos++)
            if (bits[pos])
                entropy[pos / 8] |= 0x80 >>> (pos % 8);

        // the next (startOfChecksum - startOfTransaction) bits are the transaction reference of the account reference
        for ( ; pos < startOfChecksum; pos++)
            if (bits[pos]) {
                int temp = pos - startOfTransaction;
                transaction[temp / 8] |= 0x80 >>> (temp % 8);
            }

        // the remaining bits are the checksum
        for ( ; pos < bits.length; pos++)
            checksum[pos - startOfChecksum] = bits[pos];

        // we recompute the checksum from entropy and transaction
        MessageDigest digest;

        try {
        	digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
        	throw new RuntimeException("unexpected exception", e);
        }

        var merge = new byte[entropy.length + transaction.length];
    	System.arraycopy(entropy, 0, merge, 0, entropy.length);
    	System.arraycopy(transaction, 0, merge, entropy.length, transaction.length);
        byte[] sha256 = digest.digest(merge);
        var checksumRecomputed = new boolean[bitsOfChecksum];
        for (pos = 0; pos < bitsOfChecksum; pos++)
            checksumRecomputed[pos] = (sha256[pos] & (0x80 >>> (pos % 8))) != 0;

        if (!Arrays.equals(checksum, checksumRecomputed))
            throw new IllegalArgumentException("illegal mnemonic phrase: checksum mismatch");

        return accountCreator.apply(io.hotmoka.crypto.Entropies.of(entropy), transaction);
    }

    @Override
    public void dump(Path name) throws IOException {
    	try (var writer = new PrintWriter(name.toFile())) {
    		for (String word: words)
    			writer.println(word);
    	}
	}

	/**
     * Transforms a sequence of bytes into BIP39 words, including a checksum at its end.
     * 
     * @param data the bytes
     * @param words the list where words get added
     * @return the final value of {@code words}, as an array
     */
    private String[] words(byte[] data, List<String> words) {
        MessageDigest digest;

        try {
        	digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
        	throw new RuntimeException("unexpected exception", e);
        }

        var sha256 = digest.digest(data);

        // we represent the bits of data as an array of bits, followed by the data.size / 32 bits for the checksum
        var dataSizeTimes8 = data.length * 8;
        var bits = new boolean[dataSizeTimes8 + dataSizeTimes8 / 32];

        // the initial bits are those of data
        for (int pos = 0; pos < dataSizeTimes8; pos++)
            bits[pos] = (data[pos / 8] & (0x80 >>> (pos % 8))) != 0;

        // the remaining bits are the first (bits.size - dataSizeTimes8) bits of the sha256 checksum
        for (int pos = dataSizeTimes8; pos < bits.length; pos++)
            bits[pos] = (sha256[pos - dataSizeTimes8] & (0x80 >>> (pos % 8))) != 0;

        selectWordsFor(bits, words);

        return words.toArray(new String[words.size()]); // old form to make Android happy
    }

    /**
     * Transforms a sequence of bits into BIP39 words.
     * 
     * @param bits the bits
     * @param words the list where words get added
     */
    private void selectWordsFor(boolean[] bits, List<String> words) {
        // we take 11 bits at a time from bits and use them as an index into the dictionary
        for (int pos = 0; pos < bits.length - 10; pos += 11) {
            // we select bits from pos (inclusive) to pos + 11 (exclusive)
            int index = 0;
            for (int pos2 = 0; pos2 <= 10; pos2++)
                if (bits[pos + pos2])
                    index |= 0x0400 >>> pos2;

            // we interpret the 11 bits selection as the index inside the dictionary
            // and we add the index-th word from the dictionary
            words.add(dictionary.getWord(index));
        }
    }
}