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

import io.hotmoka.crypto.BytesSupplier;
import io.hotmoka.crypto.HashingAlgorithm;

/**
 * The SHA256 hashing algorithm.
 * 
 * @param <T> the type of values that get hashed
 */
public class SHA256<T> implements HashingAlgorithm<T>{

	private final MessageDigest digest;

	/**
	 * How values get transformed into bytes, before being hashed.
	 */
	private final BytesSupplier<? super T> supplier;

	public SHA256(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		this.digest = MessageDigest.getInstance("SHA-256");
		this.supplier = supplier;
	}

	@Override
	public byte[] hash(T what) {
		try {
			byte[] bytes = supplier.get(what);

			synchronized (digest) {
				digest.reset();
				return digest.digest(bytes);
			}
		}
		catch(Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public int length() {
		return 32;
	}
}