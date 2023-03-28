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

package io.hotmoka.stores.internal;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.crypto.AbstractHashingAlgorithm;

/**
 * The hashing algorithm applied to transaction references when used as
 * keys of the trie. Since these keys are transaction references,
 * they already hold a hash, as a string. Hence, this algorithm just amounts to extracting
 * the bytes from that string.
 */
class HashingForTransactionReference extends AbstractHashingAlgorithm<TransactionReference> {

    @Override
    public byte[] hash(TransactionReference reference) {
        return hexStringToByteArray(reference.getHash());
    }

	@Override
    public int length() {
        return 32; // transaction references are assumed to be SHA256 hashes, hence 32 bytes
    }

    /**
     * Transforms a hexadecimal string into a byte array.
     *
     * @param s the string
     * @return the byte array
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len - 1; i += 2)
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));

        return data;
    }

	@Override
	public String getName() {
		return "custom";
	}
}