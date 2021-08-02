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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.crypto.Account;
import io.hotmoka.crypto.BIP39Dictionary;
import io.hotmoka.crypto.BIP39Words;

public class BIP39WordsImpl implements BIP39Words {
	private final BIP39Dictionary dictionary;
    private final String[] words;

    public BIP39WordsImpl(Account account, BIP39Dictionary dictionary) {
    	this.dictionary = dictionary;
    	var transaction = account.transaction;

    	if (transaction == null)
    		throw new IllegalArgumentException("Cannot compute BIP39 words if the reference is not set");

    	byte[] entropy = account.getEntropy();
    	byte[] transactionBytes = transaction.getHashAsBytes();
    	byte[] merge = new byte[entropy.length + transactionBytes.length];
    	System.arraycopy(entropy, 0, merge, 0, entropy.length);
    	System.arraycopy(transactionBytes, 0, merge, entropy.length, transactionBytes.length);
        this.words = words(merge, new ArrayList<>());
    }

    public BIP39WordsImpl(byte[] entropy, BIP39Dictionary dictionary) {
    	this.dictionary = dictionary;
        this.words = words(entropy, new ArrayList<>());
    }

    public BIP39WordsImpl(String[] words, BIP39Dictionary dictionary) {
    	this.words = words.clone();
    	this.dictionary = dictionary;
    }

    @Override
    public Stream<String> stream() {
        return Stream.of(words);
    }

    @Override
    public Account toAccount() {
        if (words.length != 36)
            throw new IllegalArgumentException("expected 36 mnemonic words rather than " + words.length);

        // each mnemonic word represents 11 bits
        boolean[] bits = new boolean[words.length * 11];
        byte[] entropy = new byte[16];
        int startOfTransaction = entropy.length * 8;
        byte[] transaction = new byte[32];
        int startOfChecksum = startOfTransaction + transaction.length * 8;
        int bitsOfChecksum = words.length / 3;
        boolean[] checksum = new boolean[bitsOfChecksum];

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

        // the first 16 * 8 bits are the entropy
        for (pos = 0; pos < startOfTransaction; pos++)
            if (bits[pos])
                entropy[pos / 8] |= 0x80 >>> (pos % 8);

        // the next 32 * 8 bits are the transaction reference of the account reference
        for ( ; pos < startOfChecksum; pos++)
            if (bits[pos]) {
                int temp = pos - startOfTransaction;
                transaction[temp / 8] |= 0x80 >>> (temp % 8);
            }

        // the remaining bits are the checksum
        for ( ; pos < bits.length; pos++)
            checksum[pos - startOfChecksum] = bits[pos];

        // the recompute the checksum from entropy and transaction
        MessageDigest digest;

        try {
        	digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
        	throw InternalFailureException.of("unexpected exception", e);
        }

        byte[] merge = new byte[entropy.length + transaction.length];
    	System.arraycopy(entropy, 0, merge, 0, entropy.length);
    	System.arraycopy(transaction, 0, merge, entropy.length, transaction.length);
        byte[] sha256 = digest.digest(merge);
        boolean[] checksumRecomputed = new boolean[bitsOfChecksum];
        for (pos = 0; pos < bitsOfChecksum; pos++)
            checksumRecomputed[pos] = (sha256[pos] & (0x80 >>> (pos % 8))) != 0;

        if (!Arrays.equals(checksum, checksumRecomputed))
            throw new IllegalArgumentException("illegal mnemonic phrase: checksum mismatch");

        return new Account(entropy, new LocalTransactionReference(transaction));
    }

    private String[] words(byte[] data, List<String> words) {
        MessageDigest digest;

        try {
        	digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
        	throw InternalFailureException.of("unexpected exception", e);
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

        return words.toArray(String[]::new);
    }

    private void selectWordsFor(boolean[] bits, List<String> words) {
        // we take 11 bits at a time from bits and use them as an index into allWords
        for (int pos = 0; pos < bits.length - 10; pos += 11) {
            // we select bits from pos (inclusive) to pos + 11 (exclusive)
            int index = 0;
            for (int pos2 = 0; pos2 <= 10; pos2++)
                if (bits[pos + pos2])
                    index = index | (0x0400 >>> pos2);

            // we interpret the 11 bits selection as the index inside the dictionary
            // we add the index-th word from the dictionary
            words.add(dictionary.getWord(index));
        }
    }
}