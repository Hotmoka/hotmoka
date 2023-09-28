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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * A dummy signature algorithm that signs everything with an empty array of bytes.
 * 
 * @param <T> the type of values that get signed
 */
public class EMPTY<T> extends AbstractSignatureAlgorithmImpl<T> {
	private final static byte[] EMPTY = new byte[0];

	private final KeyPair dummyKeys = new KeyPair(
	new PublicKey() {
		private static final long serialVersionUID = 1L;
		
		@Override
		public String getFormat() {
			return "empty";
		}
		
		@Override
		public byte[] getEncoded() {
			return EMPTY;
		}
		
		@Override
		public String getAlgorithm() {
			return "empty";
		}
	},
	new PrivateKey() {
		private static final long serialVersionUID = 1L;
		
		@Override
		public String getFormat() {
			return "empty";
		}
		
		@Override
		public byte[] getEncoded() {
			return EMPTY;
		}
		
		@Override
		public String getAlgorithm() {
			return "empty";
		}
	});

	public EMPTY() {
	}

	@Override
	protected KeyPairGenerator mkKeyPairGenerator(SecureRandom random)
			throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return null;
	}

	@Override
	public KeyPair getKeyPair() {
		return dummyKeys;
	}

	@Override
	protected byte[] sign(T what, PrivateKey privateKey) {
		return EMPTY;
	}

	@Override
	public boolean verify(T what, PublicKey publicKey, byte[] signature) {
		return Arrays.equals(EMPTY, signature);
	}

	@Override
	public PublicKey publicKeyFromEncoding(byte[] encoded) {
		return dummyKeys.getPublic();
	}

	@Override
	public PrivateKey privateKeyFromEncoding(byte[] encoded) throws InvalidKeySpecException {
		return dummyKeys.getPrivate();
	}

	@Override
	public String getName() {
		return "empty";
	}
}