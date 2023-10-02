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

import io.hotmoka.crypto.AbstractHashingAlgorithm;

/**
 * The SHA256 hashing algorithm.
 */
public class SHA256 extends AbstractHashingAlgorithm {

	private final MessageDigest digest;

	public SHA256() throws NoSuchAlgorithmException {
		this.digest = MessageDigest.getInstance("SHA-256");
	}

	@Override
	protected byte[] hash(byte[] bytes) {
		synchronized (digest) {
			digest.reset();
			return digest.digest(bytes);
		}
	}

	@Override
	protected byte[] hash(byte[] bytes, int start, int length) {
		synchronized (digest) {
			digest.reset();
			digest.update(bytes, start, length);
			return digest.digest();
		}
	}

	@Override
	public int length() {
		return 32;
	}

	@Override
	public SHA256 clone() {
		try {
			return new SHA256();
		}
		catch (NoSuchAlgorithmException e) {
			// impossible, since this was already created successfully, unless the provider has been removed
			throw new RuntimeException("Cannot clone SHA256 since the provider is not available");
		}
	}
}